package com.hakku.main.wishlist.domain;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 찜에 대한 좋아요 (요구사항 §2). 찜-회원 당 한 번만(유니크).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WishlistLike {

    private Long id;

    private Long wishlistId;

    private Long userId;

    private Instant createdAt;

    public WishlistLike(Long wishlistId, Long userId) {
        this.wishlistId = wishlistId;
        this.userId = userId;
    }

    /** 영속 시각을 부여한다(MyBatis insert 직전 호출). JPA @CreationTimestamp 대체. */
    public void assignCreationTime(Instant now) {
        this.createdAt = now;
    }
}
