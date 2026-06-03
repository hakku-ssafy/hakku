package com.hakku.main.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 상품 리뷰 (PRD §3.3). 평점은 1~5, 회원-상품 당 한 번만(유니크). 평점은 추천 엔진(§3.5)의 reviewScore 피처가 된다.
 */
@Entity
@Table(name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "author_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
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

    private static int requireValidRating(int rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException("평점은 " + MIN_RATING + "~" + MAX_RATING + " 사이여야 합니다: " + rating);
        }
        return rating;
    }
}
