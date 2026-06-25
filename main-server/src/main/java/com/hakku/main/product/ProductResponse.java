package com.hakku.main.product;

import com.hakku.main.product.domain.Product;
import java.time.Instant;
import java.util.Set;

public record ProductResponse(
        Long id,
        Long sellerId,
        String name,
        String description,
        long price,
        String category,
        String keyColor,
        String subColor,
        Set<String> colors,
        Set<String> styles,
        String imageUrl,
        String purchaseUrl,
        Instant createdAt,
        Instant updatedAt,
        boolean active) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSellerId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory(),
                product.getKeyColor(),
                product.getSubColor(),
                Set.copyOf(product.getColors()),
                Set.copyOf(product.getStyles()),
                product.getImageUrl(),
                product.getPurchaseUrl(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.isActive());
    }
}
