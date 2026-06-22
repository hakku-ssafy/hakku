package com.hakku.main.review.repository;

import com.hakku.main.review.repository.ReviewRepository.ProductRatingAverage;

/**
 * {@link ReviewRepository#findAverageRatingByProduct()} 의 MyBatis 매핑 대상 구현체.
 *
 * <p>{@link ProductRatingAverage} 인터페이스(기존 JPA 사영 인터페이스)는 그대로 유지하고
 * (RecommendationService 호출부/테스트 호환), MyBatis 는 {@code resultType} 으로 이 구상 클래스를
 * 만든다. 컬럼 {@code product_id → productId}, {@code average → average} (mapUnderscoreToCamelCase).
 */
public class ProductRatingAverageRow implements ProductRatingAverage {

    private Long productId;
    private Double average;

    @Override
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    @Override
    public Double getAverage() {
        return average;
    }

    public void setAverage(Double average) {
        this.average = average;
    }
}
