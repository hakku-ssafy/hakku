package com.hakku.main.recommendation.domain;

/**
 * Immutable breakdown of a single product's recommendation score (PRD §3.5):
 *
 * <pre>total = personalColor + style + recentAction + popularity + review</pre>
 *
 * Keeping the components (not just the total) makes the score explainable and
 * lets each contribution be asserted independently in tests.
 */
public record RecommendationScore(
        double personalColor,
        double style,
        double recentAction,
        double popularity,
        double review
) {
    public double total() {
        return personalColor + style + recentAction + popularity + review;
    }
}
