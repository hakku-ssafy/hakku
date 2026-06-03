package com.hakku.main.product;

import com.hakku.main.product.domain.Product;
import java.time.Instant;
import java.util.Set;

/**
 * 상품 응답 DTO.
 */
public record ProductResponse(
        Long id,
        Long sellerId,
        String name,
        String description,
        long price,
        String keyColor,
        String subColor,
        Set<String> styles,
        String imageUrl,
        Instant createdAt,
        Instant updatedAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSellerId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getKeyColor(),
                product.getSubColor(),
                Set.copyOf(product.getStyles()),
                product.getImageUrl(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
