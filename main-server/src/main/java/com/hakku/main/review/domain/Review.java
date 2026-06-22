package com.hakku.main.review.domain;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 리뷰 (PRD §3.3). 평점은 1~5, 회원-상품 당 한 번만(유니크). 평점은 추천 엔진(§3.5)의 reviewScore 피처가 된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    private Long id;

    private Long productId;

    private Long authorId;

    private int rating;

    private String content;

    private Instant createdAt;

    private Instant updatedAt;

    public Review(Long productId, Long authorId, int rating, String content) {
        this.productId = productId;
        this.authorId = authorId;
        this.rating = requireValidRating(rating);
        this.content = content;
    }

    /** 평점/내용을 갱신한다. */
    public void update(int rating, String content) {
        this.rating = requireValidRating(rating);
        this.content = content;
    }

    /** 주어진 회원이 이 리뷰의 작성자인지 여부. */
    public boolean isAuthor(Long userId) {
        return this.authorId.equals(userId);
    }

    /** 영속 시각을 부여한다(MyBatis insert 직전 호출). JPA @CreationTimestamp 대체. createdAt/updatedAt 동시 설정. */
    public void assignCreationTime(Instant now) {
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 갱신 시각을 부여한다(MyBatis update 직전 호출). JPA @UpdateTimestamp 대체. */
    public void assignUpdateTime(Instant now) {
        this.updatedAt = now;
    }

    private static int requireValidRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException("평점은 " + MIN_RATING + "~" + MAX_RATING + " 사이여야 합니다: " + rating);
        }
        return rating;
    }
}
