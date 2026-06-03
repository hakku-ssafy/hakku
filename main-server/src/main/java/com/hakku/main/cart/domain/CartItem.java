package com.hakku.main.cart.domain;

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
 * 장바구니 항목 (PRD §3.3). 회원-상품 당 한 행(유니크), 수량으로 묶는다.
 */
@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
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

    private static int requirePositive(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다: " + value);
        }
        return value;
    }
}
