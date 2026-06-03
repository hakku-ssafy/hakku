package com.hakku.main.recommendation.domain;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 퍼스널컬러 계절(SPRING/SUMMER/…)과 상품 색상 태그(red, blue 등) 매칭.
 */
public final class SeasonColorAffinity {

    private static final Map<String, Set<String>> SEASON_COLORS = Map.of(
            "SPRING", Set.of("pink", "orange", "yellow", "red", "green"),
            "SUMMER", Set.of("blue", "purple", "pink", "green"),
            "AUTUMN", Set.of("brown", "orange", "yellow", "green", "red"),
            "WINTER", Set.of("blue", "purple", "pink", "red"));

    private SeasonColorAffinity() {
    }

    public static boolean matchesAnyProductColor(String season, Set<String> productColors) {
        if (season == null || productColors == null || productColors.isEmpty()) {
            return false;
        }
        Set<String> palette = SEASON_COLORS.get(season.toUpperCase(Locale.ROOT));
        if (palette == null) {
            return false;
        }
        return productColors.stream()
                .anyMatch(color -> palette.contains(color.toLowerCase(Locale.ROOT)));
    }
}
