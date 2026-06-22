package com.hakku.main.wishlist.domain;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 찜 (요구사항 §2). 회원-상품 당 한 번만(유니크). 다른 회원이 이 찜에 좋아요를 누를 수 있다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wishlist {

    private Long id;

    private Long productId;

    private Long userId;

    private Instant createdAt;

    public Wishlist(Long productId, Long userId) {
        this.productId = productId;
        this.userId = userId;
    }

    /** 영속 시각을 부여한다(MyBatis insert 직전 호출). JPA @CreationTimestamp 대체. */
    public void assignCreationTime(Instant now) {
        this.createdAt = now;
    }
}
