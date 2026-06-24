package com.hakku.main.order.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 주문 항목 스냅샷. 주문 시점의 상품명/가격을 박제해 이후 상품 변경/삭제와 무관하게 보존한다. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private long price;
    private int quantity;
    private long lineTotal;

    public OrderItem(Long productId, String productName, long price, int quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("상품 id는 필수입니다.");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (price < 0) {
            throw new IllegalArgumentException("가격은 0 이상이어야 합니다: " + price);
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다: " + quantity);
        }
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.lineTotal = price * quantity;
    }

    /** insert 직전, 부모 주문 id를 부여한다. */
    public void assignOrderId(Long orderId) {
        this.orderId = orderId;
    }
}
