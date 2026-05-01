package com.musicrhythmatch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * All communication with the Spotify Web API lives here.
 * Each method is a thin wrapper around a WebClient call.
 * We use .block() to stay in the synchronous Spring MVC world — fine for MVP.
 */
@Service
@Slf4j
public class SpotifyApiService {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final WebClient webClient;

    public SpotifyApiService(WebClient spotifyWebClient) {
        this.webClient = spotifyWebClient;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API CALLS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * GET /v1/me/top/tracks?time_range=medium_term&limit=50
     * Returns the user's top 50 tracks over the last ~6 months.
     */
    public Map<String, Object> getTopTracks(String accessToken) {
        log.info("Fetching top tracks from Spotify...");
        return callSpotify("/me/top/tracks?time_range=medium_term&limit=50", accessToken);
    }

    /**
     * GET /v1/me/top/artists?time_range=medium_term&limit=50
     * Returns the user's top 50 artists (we extract genres from these).
     */
    public Map<String, Object> getTopArtists(String accessToken) {
        log.info("Fetching top artists from Spotify...");
        return callSpotify("/me/top/artists?time_range=medium_term&limit=50", accessToken);
    }

    /**
     * GET /v1/audio-features?ids=id1,id2,...
     * Returns audio feature objects (energy, danceability, etc.) for a batch of track IDs.
     * Spotify allows max 100 IDs per request.
     */
    public Map<String, Object> getAudioFeatures(String accessToken, List<String> trackIds) {
        String ids = String.join(",", trackIds);
        log.info("Fetching audio features for {} tracks...", trackIds.size());
        return callSpotify("/audio-features?ids=" + ids, accessToken);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATA EXTRACTION HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Pulls the track ID strings out of a top-tracks API response.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractTrackIds(Map<String, Object> topTracksResponse) {
        if (topTracksResponse == null) return Collections.emptyList();
        List<Map<String, Object>> items = (List<Map<String, Object>>) topTracksResponse.get("items");
        if (items == null) return Collections.emptyList();

        return items.stream()
                .map(item -> (String) item.get("id"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Averages the 4 key audio features across all tracks.
     * Returns double[] { energy, danceability, valence, acousticness }
     * Defaults to 0.5 for each dimension if no data is available.
     */
    @SuppressWarnings("unchecked")
    public double[] computeAverageAudioFeatures(Map<String, Object> audioFeaturesResponse) {
        if (audioFeaturesResponse == null) return defaultVector();
        List<Map<String, Object>> features =
                (List<Map<String, Object>>) audioFeaturesResponse.get("audio_features");
        if (features == null || features.isEmpty()) return defaultVector();

        // Spotify can return null entries for tracks without audio data — filter them
        List<Map<String, Object>> valid = features.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (valid.isEmpty()) return defaultVector();

        double energy       = valid.stream().mapToDouble(f -> asDouble(f, "energy")).average().orElse(0.5);
        double danceability = valid.stream().mapToDouble(f -> asDouble(f, "danceability")).average().orElse(0.5);
        double valence      = valid.stream().mapToDouble(f -> asDouble(f, "valence")).average().orElse(0.5);
        double acousticness = valid.stream().mapToDouble(f -> asDouble(f, "acousticness")).average().orElse(0.5);

        log.info("Computed audio averages → energy={}, dance={}, valence={}, acoustic={}",
                round(energy), round(danceability), round(valence), round(acousticness));

        return new double[]{ energy, danceability, valence, acousticness };
    }

    /**
     * Tallies genre occurrences across all top artists and returns a
     * comma-separated string of the top 10 genres by frequency.
     * E.g., "indie pop,alternative rock,dream pop"
     */
    @SuppressWarnings("unchecked")
    public String extractTopGenres(Map<String, Object> topArtistsResponse) {
        if (topArtistsResponse == null) return "";
        List<Map<String, Object>> items =
                (List<Map<String, Object>>) topArtistsResponse.get("items");
        if (items == null) return "";

        Map<String, Long> genreCount = new HashMap<>();
        for (Map<String, Object> artist : items) {
            List<String> genres = (List<String>) artist.get("genres");
            if (genres != null) {
                genres.forEach(g -> genreCount.merge(g, 1L, Long::sum));
            }
        }

        return genreCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(","));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE INTERNALS
    // ─────────────────────────────────────────────────────────────────────────

    private Map<String, Object> callSpotify(String path, String accessToken) {
        try {
            Map<String, Object> result = webClient.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(MAP_TYPE)
                    .block();
            return result != null ? result : Collections.emptyMap();
        } catch (WebClientResponseException e) {
            log.error("Spotify API error [{}] on path {}: {}", e.getStatusCode(), path, e.getResponseBodyAsString());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Unexpected error calling Spotify path {}: {}", path, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private double asDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : 0.5;
    }

    private double[] defaultVector() {
        return new double[]{ 0.5, 0.5, 0.5, 0.5 };
    }

    private String round(double d) {
        return String.format("%.3f", d);
    }
}