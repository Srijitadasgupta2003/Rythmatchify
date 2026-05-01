package com.musicrhythmatch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single user's musical "DNA" stored in Supabase.
 * The taste vector [energy, danceability, valence, acousticness] is the
 * mathematical foundation for all soulmate/nemesis calculations.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /** Spotify's own unique ID for the user (e.g., "31xyzabc...") */
    @Id
    @Column(name = "spotify_id", nullable = false)
    private String spotifyId;

    /** Display name from Spotify profile */
    @Column(name = "display_name")
    private String displayName;

    /** Profile picture URL from Spotify */
    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    /**
     * Comma-separated top genres derived from user's top artists.
     * E.g.: "indie pop,alternative rock,dream pop,shoegaze"
     */
    @Column(name = "top_genres", columnDefinition = "TEXT")
    private String topGenres;

    /** Average energy of top 50 tracks (0.0 = calm, 1.0 = intense) */
    @Column(name = "avg_energy")
    private Double avgEnergy;

    /** Average danceability of top 50 tracks (0.0 = least, 1.0 = most) */
    @Column(name = "avg_danceability")
    private Double avgDanceability;

    /** Average valence (happiness) of top 50 tracks (0.0 = sad, 1.0 = euphoric) */
    @Column(name = "avg_valence")
    private Double avgValence;

    /** Average acousticness of top 50 tracks (0.0 = electronic, 1.0 = purely acoustic) */
    @Column(name = "avg_acousticness")
    private Double avgAcousticness;

    /** Timestamp of the last login — taste vectors are refreshed on every login */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
}