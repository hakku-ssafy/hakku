package com.hakku.main.product;

import com.hakku.main.product.domain.Product;
import java.time.Instant;
import java.util.Set;

public record ProductResponse(
        Long id,
        Long sellerId,
        String sellerNickname,
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

    /** 판매자 닉네임을 알 수 없는 맥락(예: 추천 응답)에서는 null 로 둔다. */
    public static ProductResponse from(Product product) {
        return from(product, null);
    }

    public static ProductResponse from(Product product, String sellerNickname) {
        return new ProductResponse(
                product.getId(),
                product.getSellerId(),
                sellerNickname,
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
