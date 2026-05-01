package com.musicrhythmatch.dto;

import com.musicrhythmatch.entity.User;
import lombok.Getter;

/**
 * Wraps a candidate User together with their computed similarity score.
 * Used by Thymeleaf templates to render soulmate and nemesis cards.
 */
@Getter
public class MatchResult {

    private final User user;

    /**
     * Combined similarity score in range [0.0, 1.0].
     * Higher = more similar (soulmate), lower = less similar (nemesis).
     */
    private final double score;

    public MatchResult(User user, double score) {
        this.user = user;
        this.score = score;
    }

    /**
     * Score as a clean integer percentage for display.
     * E.g., 0.876 → 88
     */
    public int getScorePercentage() {
        return (int) Math.round(score * 100);
    }

    /**
     * Inverse score — how DIFFERENT this nemesis is.
     * E.g., score 0.12 → differencePercentage = 88 (88% different)
     */
    public int getDifferencePercentage() {
        return (int) Math.round((1.0 - score) * 100);
    }
}