package com.hakku.main.recommendation.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the content-based recommendation scorer (PRD §3.5):
 * total = personalColor + style + recentAction + popularity + review
 */
class RecommendationScoreCalculatorTest {

    private static final double EPS = 1e-9;

    private final RecommendationScoreCalculator calculator = new RecommendationScoreCalculator();

    @Nested
    @DisplayName("personal color")
    class PersonalColor {

        @Test
        @DisplayName("key color match scores higher than sub color match")
        void keyColorMatchScoresFull() {
            var user = new UserPreferenceProfile("WINTER", Set.of(), Set.of());
            var keyMatch = new ProductFeatures("WINTER", "SUMMER", Set.of(), 0.0, 0.0);
            var subMatch = new ProductFeatures("SPRING", "WINTER", Set.of(), 0.0, 0.0);

            assertEquals(3.0, calculator.score(user, keyMatch).personalColor(), EPS);
            assertEquals(1.5, calculator.score(user, subMatch).personalColor(), EPS);
        }

        @Test
        @DisplayName("no color match scores zero")
        void noColorMatch() {
            var user = new UserPreferenceProfile("WINTER", Set.of(), Set.of());
            var product = new ProductFeatures("SPRING", "SUMMER", Set.of(), 0.0, 0.0);

            assertEquals(0.0, calculator.score(user, product).personalColor(), EPS);
        }

        @Test
        @DisplayName("match is case-insensitive")
        void caseInsensitive() {
            var user = new UserPreferenceProfile("winter", Set.of(), Set.of());
            var product = new ProductFeatures("WINTER", null, Set.of(), 0.0, 0.0);

            assertEquals(3.0, calculator.score(user, product).personalColor(), EPS);
        }
    }

    @Nested
    @DisplayName("style overlap")
    class Style {

        @Test
        @DisplayName("full overlap scores full style weight")
        void fullOverlap() {
            var user = new UserPreferenceProfile(null, Set.of("cute", "pastel"), Set.of());
            var product = new ProductFeatures(null, null, Set.of("cute", "pastel"), 0.0, 0.0);

            assertEquals(2.0, calculator.score(user, product).style(), EPS);
        }

        @Test
        @DisplayName("partial overlap scores by fraction of user preferences covered")
        void partialOverlap() {
            var user = new UserPreferenceProfile(null, Set.of("a", "b", "c", "d"), Set.of());
            var product = new ProductFeatures(null, null, Set.of("a", "b"), 0.0, 0.0);

            // 2 of 4 preferences covered -> 0.5 * 2.0 weight
            assertEquals(1.0, calculator.score(user, product).style(), EPS);
        }

        @Test
        @DisplayName("no preferred styles scores zero (no divide-by-zero)")
        void noPreferences() {
            var user = new UserPreferenceProfile(null, Set.of(), Set.of());
            var product = new ProductFeatures(null, null, Set.of("cute"), 0.0, 0.0);

            assertEquals(0.0, calculator.score(user, product).style(), EPS);
        }
    }

    @Nested
    @DisplayName("recent action tags")
    class RecentAction {

        @Test
        @DisplayName("each product style matching a recent action tag adds weight")
        void recentTagsAccumulate() {
            var user = new UserPreferenceProfile(null, Set.of(), Set.of("cute", "retro"));
            var product = new ProductFeatures(null, null, Set.of("cute", "retro", "pastel"), 0.0, 0.0);

            // 2 matches * 1.0 each
            assertEquals(2.0, calculator.score(user, product).recentAction(), EPS);
        }
    }

    @Nested
    @DisplayName("popularity and review")
    class PopularityAndReview {

        @Test
        @DisplayName("popularity contributes proportionally and is clamped to [0,1]")
        void popularityClamped() {
            var user = new UserPreferenceProfile(null, Set.of(), Set.of());
            var half = new ProductFeatures(null, null, Set.of(), 0.5, 0.0);
            var over = new ProductFeatures(null, null, Set.of(), 5.0, 0.0);

            assertEquals(0.5, calculator.score(user, half).popularity(), EPS);
            assertEquals(1.0, calculator.score(user, over).popularity(), EPS);
        }

        @Test
        @DisplayName("review contributes normalized to /5 and is clamped to [0,5]")
        void reviewNormalized() {
            var user = new UserPreferenceProfile(null, Set.of(), Set.of());
            var good = new ProductFeatures(null, null, Set.of(), 0.0, 4.0);

            assertEquals(0.8, calculator.score(user, good).review(), EPS);
        }
    }

    @Test
    @DisplayName("perfect match sums every component into the total")
    void perfectMatchTotal() {
        var user = new UserPreferenceProfile("WINTER", Set.of("cute", "pastel"), Set.of("cute"));
        var product = new ProductFeatures("WINTER", "SUMMER", Set.of("cute", "pastel"), 1.0, 5.0);

        // 3.0 (key) + 2.0 (style) + 1.0 (1 recent tag) + 1.0 (pop) + 1.0 (review) = 8.0
        assertEquals(8.0, calculator.score(user, product).total(), EPS);
    }

    @Test
    @DisplayName("completely unrelated product scores zero")
    void noSignalTotal() {
        var user = new UserPreferenceProfile("WINTER", Set.of("cute"), Set.of("retro"));
        var product = new ProductFeatures("SPRING", "AUTUMN", Set.of("formal"), 0.0, 0.0);

        assertEquals(0.0, calculator.score(user, product).total(), EPS);
    }
}
