package com.hakku.main.wishlist.domain;

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

/**
 * 찜에 대한 좋아요 (요구사항 §2). 찜-회원 당 한 번만(유니크).
 */
@Entity
@Table(name = "wishlist_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"wishlist_id", "user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WishlistLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wishlist_id", nullable = false)
    private Long wishlistId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public WishlistLike(Long wishlistId, Long userId) {
        this.wishlistId = wishlistId;
        this.userId = userId;
    }
}
