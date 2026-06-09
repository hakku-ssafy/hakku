package com.hakku.main.review;

import com.hakku.main.review.domain.Review;
import java.time.Instant;

/**
 * 리뷰 응답 DTO. 작성자 닉네임과 상품명을 함께 담아 마이페이지/상품 상세에서 바로 표시할 수 있게 한다.
 */
public record ReviewResponse(
        Long id,
        Long productId,
        String productName,
        Long authorId,
        String authorNickname,
        int rating,
        String content,
        Instant createdAt,
        Instant updatedAt) {

    public static ReviewResponse from(Review review, String authorNickname, String productName) {
        return new ReviewResponse(
                review.getId(),
                review.getProductId(),
                productName,
                review.getAuthorId(),
                authorNickname,
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
