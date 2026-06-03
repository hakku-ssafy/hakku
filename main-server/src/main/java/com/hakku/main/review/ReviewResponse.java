package com.hakku.main.review;

import com.hakku.main.review.domain.Review;
import java.time.Instant;

/**
 * 리뷰 응답 DTO.
 */
public record ReviewResponse(
        Long id,
        Long productId,
        Long authorId,
        int rating,
        String content,
        Instant createdAt,
        Instant updatedAt) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getProductId(),
                review.getAuthorId(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
