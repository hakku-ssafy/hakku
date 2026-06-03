package com.hakku.main.recommendation.domain;

import java.util.Set;

public record ProductFeatures(
        String keyColor,
        String subColor,
        Set<String> colors,
        Set<String> styles,
        double popularity,
        double reviewScore
) {
    public ProductFeatures {
        colors = colors == null ? Set.of() : Set.copyOf(colors);
        styles = styles == null ? Set.of() : Set.copyOf(styles);
    }
}
