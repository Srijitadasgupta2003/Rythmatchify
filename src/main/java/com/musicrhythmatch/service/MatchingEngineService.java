package com.musicrhythmatch.service;

import com.musicrhythmatch.dto.MatchResult;
import com.musicrhythmatch.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║             THE HEART OF MUSIC RHYTHM MATCH                     ║
 * ║                                                                  ║
 * ║  This service implements the DSA matching algorithm entirely     ║
 * ║  in Java, in-memory, with no database extensions needed.        ║
 * ║                                                                  ║
 * ║  Algorithm:                                                      ║
 * ║    1. Extract 4D taste vector from each User entity             ║
 * ║    2. Compute Cosine Similarity on audio features               ║
 * ║    3. Compute Jaccard Similarity on genre sets                  ║
 * ║    4. Combine: score = 0.6 × cosine + 0.4 × jaccard            ║
 * ║    5. Sort → top 5 (soulmates) or bottom 5 (nemeses)           ║
 * ║                                                                  ║
 * ║  Time Complexity:  O(n) per user query                          ║
 * ║  Space Complexity: O(n) for the candidate list                  ║
 * ╚══════════════════════════════════════════════════════════════════╝
 */
@Service
@Slf4j
public class MatchingEngineService {

    private static final int TOP_K = 5;

    // Weights for the combined score
    private static final double COSINE_WEIGHT  = 0.6;
    private static final double JACCARD_WEIGHT = 0.4;

    /**
     * Find the TOP_K users most similar to the current user.
     * Sort by score DESCENDING → highest similarity first.
     */
    public List<MatchResult> findSoulmates(User currentUser, List<User> pool) {
        log.info("Running soulmate search for {} across {} candidates",
                currentUser.getDisplayName(), pool.size());

        return pool.stream()
                .map(candidate -> new MatchResult(candidate, computeCombinedScore(currentUser, candidate)))
                .sorted(Comparator.comparingDouble(MatchResult::getScore).reversed())
                .limit(TOP_K)
                .collect(Collectors.toList());
    }

    /**
     * Find the TOP_K users least similar to the current user.
     * Sort by score ASCENDING → lowest similarity first.
     */
    public List<MatchResult> findNemeses(User currentUser, List<User> pool) {
        log.info("Running nemesis search for {} across {} candidates",
                currentUser.getDisplayName(), pool.size());

        return pool.stream()
                .map(candidate -> new MatchResult(candidate, computeCombinedScore(currentUser, candidate)))
                .sorted(Comparator.comparingDouble(MatchResult::getScore))
                .limit(TOP_K)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SCORING ENGINE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Combines Cosine Similarity (audio features) and Jaccard Similarity (genres)
     * into a single [0.0, 1.0] score.
     */
    private double computeCombinedScore(User a, User b) {
        double cosine  = cosineSimilarity(toVector(a), toVector(b));
        double jaccard = jaccardSimilarity(a.getTopGenres(), b.getTopGenres());

        // Cosine is in [-1, 1] — normalize it to [0, 1] before weighting
        double normalizedCosine = (cosine + 1.0) / 2.0;

        double combined = (COSINE_WEIGHT * normalizedCosine) + (JACCARD_WEIGHT * jaccard);

        log.debug("Score for [{} vs {}]: cosine={}, jaccard={}, combined={}",
                a.getDisplayName(), b.getDisplayName(),
                String.format("%.3f", normalizedCosine),
                String.format("%.3f", jaccard),
                String.format("%.3f", combined));

        return combined;
    }

    /**
     * ── COSINE SIMILARITY ─────────────────────────────────────────────────────
     *
     * Measures the angle between two 4D vectors in feature space.
     *
     *             A · B
     * cos(θ) = ─────────
     *           |A| × |B|
     *
     * Range: -1.0 (opposite) to +1.0 (identical direction)
     *
     * This is better than Euclidean distance for our case because it's
     * scale-invariant: a user who listens to 80% energy tracks is treated
     * the same as a user at 80%, regardless of listening volume.
     */
    private double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) return 0.0;

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        // Guard against zero-magnitude vectors
        if (normA == 0.0 || normB == 0.0) return 0.0;

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * ── JACCARD SIMILARITY ────────────────────────────────────────────────────
     *
     * Measures genre set overlap between two users.
     *
     *              |A ∩ B|
     * J(A,B) = ─────────────
     *              |A ∪ B|
     *
     * Range: 0.0 (no shared genres) to 1.0 (identical genre sets)
     *
     * Example: A = {pop, rock, indie}, B = {indie, jazz, pop}
     *   Intersection = {pop, indie} → 2
     *   Union        = {pop, rock, indie, jazz} → 4
     *   J = 2/4 = 0.5
     */
    private double jaccardSimilarity(String genresA, String genresB) {
        if (genresA == null || genresA.isBlank() ||
                genresB == null || genresB.isBlank()) {
            return 0.0;
        }

        Set<String> setA = new HashSet<>(Arrays.asList(genresA.split(",")));
        Set<String> setB = new HashSet<>(Arrays.asList(genresB.split(",")));

        // Compute intersection size without modifying the originals
        long intersectionSize = setA.stream().filter(setB::contains).count();

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        if (union.isEmpty()) return 0.0;
        return (double) intersectionSize / union.size();
    }

    /**
     * Converts a User entity into a 4D double array (the taste vector).
     * Nulls are replaced with 0.5 (the neutral mid-point).
     */
    private double[] toVector(User user) {
        return new double[]{
                user.getAvgEnergy()       != null ? user.getAvgEnergy()       : 0.5,
                user.getAvgDanceability() != null ? user.getAvgDanceability() : 0.5,
                user.getAvgValence()      != null ? user.getAvgValence()      : 0.5,
                user.getAvgAcousticness() != null ? user.getAvgAcousticness() : 0.5
        };
    }
}