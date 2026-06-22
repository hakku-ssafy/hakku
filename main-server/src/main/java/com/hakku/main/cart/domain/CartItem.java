package com.hakku.main.cart.domain;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장바구니 항목 (PRD §3.3). 회원-상품 당 한 행(유니크), 수량으로 묶는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    private Long id;

    private Long userId;

    private Long productId;

    private int quantity;

    private Instant createdAt;

    private Instant updatedAt;

    public CartItem(Long userId, Long productId, int quantity) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = requirePositive(quantity);
    }

    /** 기존 수량에 더한다. */
    public void increaseQuantity(int amount) {
        this.quantity += requirePositive(amount);
    }

    /** 수량을 지정 값으로 변경한다. */
    public void changeQuantity(int quantity) {
        this.quantity = requirePositive(quantity);
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

    private static int requirePositive(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다: " + value);
        }
        return value;
    }
}
