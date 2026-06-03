package com.hakku.main.cart;

import com.hakku.main.cart.domain.CartItem;
import com.hakku.main.product.domain.Product;

/**
 * 장바구니 항목 응답 DTO. 상품 스냅샷(이름/가격/이미지)과 라인 합계를 포함한다.
 */
public record CartItemResponse(
        Long id,
        Long productId,
        String productName,
        long price,
        int quantity,
        long lineTotal,
        String imageUrl) {

    public static CartItemResponse from(CartItem item, Product product) {
        long price = product.getPrice();
        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
                product.getName(),
                price,
                item.getQuantity(),
                price * item.getQuantity(),
                product.getImageUrl());
    }
}
