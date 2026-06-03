package com.hakku.main.product;

import java.util.Set;

/** 상품 생성/수정 입력. */
public record ProductCommand(
        String name,
        String description,
        long price,
        String category,
        String keyColor,
        String subColor,
        Set<String> colors,
        Set<String> styles,
        String imageUrl,
        String purchaseUrl) {
}
