package com.hakku.main.recommendation.domain;

public class RecommendationScoreCalculator {

    private static final double KEY_COLOR_WEIGHT = 3.0;
    private static final double SUB_COLOR_WEIGHT = 1.5;
    private static final double PREFERRED_COLOR_WEIGHT = 2.0;
    private static final double STYLE_WEIGHT = 2.0;
    private static final double RECENT_ACTION_WEIGHT_PER_TAG = 1.0;
    private static final double POPULARITY_WEIGHT = 1.0;
    private static final double REVIEW_WEIGHT = 1.0;
    private static final double MAX_REVIEW = 5.0;

    public RecommendationScore score(UserPreferenceProfile user, ProductFeatures product) {
        return new RecommendationScore(
                personalColorScore(user, product),
                preferredColorScore(user, product),
                styleScore(user, product),
                recentActionScore(user, product),
                popularityScore(product),
                reviewScore(product));
    }

    private double personalColorScore(UserPreferenceProfile user, ProductFeatures product) {
        String personalColor = user.personalColor();
        if (personalColor == null) {
            return 0.0;
        }
        if (personalColor.equalsIgnoreCase(product.keyColor())) {
            return KEY_COLOR_WEIGHT;
        }
        if (personalColor.equalsIgnoreCase(product.subColor())) {
            return SUB_COLOR_WEIGHT;
        }
        if (SeasonColorAffinity.matchesAnyProductColor(personalColor, product.colors())) {
            return SUB_COLOR_WEIGHT;
        }
        return 0.0;
    }

    private double preferredColorScore(UserPreferenceProfile user, ProductFeatures product) {
        var preferred = user.preferredColors();
        if (preferred.isEmpty() || product.colors().isEmpty()) {
            return 0.0;
        }
        if (product.colors().contains("ALL")) {
            return PREFERRED_COLOR_WEIGHT;
        }
        long matches = preferred.stream()
                .filter(color -> product.colors().stream().anyMatch(c -> c.equalsIgnoreCase(color)))
                .count();
        if (matches == 0) {
            return 0.0;
        }
        double fraction = (double) matches / preferred.size();
        return fraction * PREFERRED_COLOR_WEIGHT;
    }

    private double styleScore(UserPreferenceProfile user, ProductFeatures product) {
        var preferred = user.preferredStyles();
        if (preferred.isEmpty()) {
            return 0.0;
        }
        long covered = preferred.stream().filter(product.styles()::contains).count();
        double fraction = (double) covered / preferred.size();
        return fraction * STYLE_WEIGHT;
    }

    private double recentActionScore(UserPreferenceProfile user, ProductFeatures product) {
        long matches = product.styles().stream().filter(user.recentActionTags()::contains).count();
        return matches * RECENT_ACTION_WEIGHT_PER_TAG;
    }

    private double popularityScore(ProductFeatures product) {
        double normalized = clamp(product.popularity(), 0.0, 1.0);
        return normalized * POPULARITY_WEIGHT;
    }

    private double reviewScore(ProductFeatures product) {
        double normalized = clamp(product.reviewScore(), 0.0, MAX_REVIEW) / MAX_REVIEW;
        return normalized * REVIEW_WEIGHT;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
