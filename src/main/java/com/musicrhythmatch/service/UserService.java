package com.musicrhythmatch.service;

import com.musicrhythmatch.entity.User;
import com.musicrhythmatch.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final SpotifyApiService spotifyApiService;

    public UserService(UserRepository userRepository, SpotifyApiService spotifyApiService) {
        this.userRepository = userRepository;
        this.spotifyApiService = spotifyApiService;
    }

    /**
     * The main entry point called from OAuth2LoginSuccessHandler.
     * Fetches all Spotify data, computes the taste vector, and upserts the user row.
     */
    public void fetchAndSaveUserProfile(String accessToken, OAuth2User principal) {
        String spotifyId = principal.getAttribute("id");
        String displayName = principal.getAttribute("display_name");

        // Extract profile image URL (Spotify returns an array of image objects)
        String profileImageUrl = extractProfileImageUrl(principal);

        // ── STEP 1: Fetch top tracks → extract IDs ────────────────────────────
        Map<String, Object> topTracks = spotifyApiService.getTopTracks(accessToken);
        List<String> trackIds = spotifyApiService.extractTrackIds(topTracks);

        // ── STEP 2: Fetch audio features → compute averages ───────────────────
        double[] avgFeatures = new double[]{ 0.5, 0.5, 0.5, 0.5 };
        if (!trackIds.isEmpty()) {
            // Spotify audio-features endpoint accepts max 100 IDs
            List<String> batchIds = trackIds.subList(0, Math.min(trackIds.size(), 100));
            Map<String, Object> audioFeatures = spotifyApiService.getAudioFeatures(accessToken, batchIds);
            avgFeatures = spotifyApiService.computeAverageAudioFeatures(audioFeatures);
        }

        // ── STEP 3: Fetch top artists → extract genre list ────────────────────
        Map<String, Object> topArtists = spotifyApiService.getTopArtists(accessToken);
        String genres = spotifyApiService.extractTopGenres(topArtists);

        // ── STEP 4: Upsert the user row (insert if new, update if returning) ──
        User user = userRepository.findById(spotifyId).orElse(new User());
        user.setSpotifyId(spotifyId);
        user.setDisplayName(displayName != null ? displayName : "Unknown Listener");
        user.setProfileImageUrl(profileImageUrl);
        user.setTopGenres(genres);
        user.setAvgEnergy(avgFeatures[0]);
        user.setAvgDanceability(avgFeatures[1]);
        user.setAvgValence(avgFeatures[2]);
        user.setAvgAcousticness(avgFeatures[3]);
        user.setLastLogin(LocalDateTime.now());

        userRepository.save(user);
        log.info("Upserted user: {} (id={})", user.getDisplayName(), spotifyId);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserById(String spotifyId) {
        return userRepository.findById(spotifyId);
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsersExcept(String spotifyId) {
        return userRepository.findAllBySpotifyIdNot(spotifyId);
    }

    public void deleteUser(String spotifyId) {
        userRepository.deleteById(spotifyId);
        log.info("Deleted user data for spotifyId={}", spotifyId);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String extractProfileImageUrl(OAuth2User principal) {
        try {
            List<Map<String, Object>> images = principal.getAttribute("images");
            if (images != null && !images.isEmpty()) {
                return (String) images.get(0).get("url");
            }
        } catch (Exception e) {
            log.warn("Could not extract profile image URL: {}", e.getMessage());
        }
        return "";
    }
}