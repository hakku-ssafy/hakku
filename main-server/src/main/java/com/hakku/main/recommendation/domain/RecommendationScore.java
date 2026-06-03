package com.hakku.main.recommendation.domain;

public record RecommendationScore(
        double personalColor,
        double preferredColor,
        double style,
        double recentAction,
        double popularity,
        double review
) {
    public double total() {
        return personalColor + preferredColor + style + recentAction + popularity + review;
    }
}
