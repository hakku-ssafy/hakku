package com.hakku.main.wishlist;

import java.time.Instant;

/**
 * 찜 응답 DTO. 상품 요약 정보와 찜 좋아요 상태를 함께 담는다.
 */
public record WishlistResponse(
        Long id,
        Long productId,
        String productName,
        String productImageUrl,
        long productPrice,
        Long ownerId,
        String ownerNickname,
        long likeCount,
        boolean likedByMe,
        Instant createdAt) {
}
