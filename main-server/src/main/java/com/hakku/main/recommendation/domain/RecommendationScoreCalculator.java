package com.hakku.main.recommendation.domain;

/**
 * Content-based recommendation scorer (PRD §3.5):
 *
 * <pre>total = personalColor + style + recentAction + popularity + review</pre>
 *
 * Each component is weighted by a named constant so the formula stays explicit
 * and free of magic numbers.
 */
public class RecommendationScoreCalculator {

    /** Product key color matches the user's personal color. */
    private static final double KEY_COLOR_WEIGHT = 3.0;
    /** Product sub color matches the user's personal color (weaker signal). */
    private static final double SUB_COLOR_WEIGHT = 1.5;
    /** Full weight when the product covers all of the user's preferred styles. */
    private static final double STYLE_WEIGHT = 2.0;
    /** Added per product style that matches a recent-action tag. */
    private static final double RECENT_ACTION_WEIGHT_PER_TAG = 1.0;
    /** Full weight at maximum popularity. */
    private static final double POPULARITY_WEIGHT = 1.0;
    /** Full weight at a perfect review average. */
    private static final double REVIEW_WEIGHT = 1.0;
    /** Maximum possible review rating, used to normalize to [0, 1]. */
    private static final double MAX_REVIEW = 5.0;

    public RecommendationScore score(UserPreferenceProfile user, ProductFeatures product) {
        return new RecommendationScore(
                personalColorScore(user, product),
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
        return 0.0;
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
