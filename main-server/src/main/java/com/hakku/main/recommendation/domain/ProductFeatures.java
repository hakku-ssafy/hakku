package com.hakku.main.recommendation.domain;

import java.util.Set;

/**
 * Immutable feature vector of a product, used as scoring input (PRD §3.5).
 *
 * @param keyColor     primary color tag of the product (e.g. "WINTER"); may be null
 * @param subColor     secondary color tag of the product; may be null
 * @param styles       style tags describing the product; never null (use empty set)
 * @param popularity   normalized popularity in [0.0, 1.0]
 * @param reviewScore  average review rating in [0.0, 5.0]
 */
public record ProductFeatures(
        String keyColor,
        String subColor,
        Set<String> styles,
        double popularity,
        double reviewScore
) {
    public ProductFeatures {
        styles = styles == null ? Set.of() : Set.copyOf(styles);
    }
}
