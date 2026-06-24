package com.hakku.main.order.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 애그리거트. 장바구니 스냅샷(항목)과 배송지를 보관한다. 결제 자체는 payment-server 가 담당하며,
 * payment.approved 이벤트를 main-server 가 수신해 {@link #markPaid()} 로 PAID 전이한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    private Long id;
    private Long userId;
    private OrderStatus status;
    private String recipientName;
    private String phone;
    private String postalCode;
    private String address1;
    private String address2;
    private long totalAmount;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrderItem> items = new ArrayList<>();

    public Order(Long userId, ShippingAddress address, List<OrderItem> items) {
        if (userId == null) {
            throw new IllegalArgumentException("회원 id는 필수입니다.");
        }
        if (address == null) {
            throw new IllegalArgumentException("배송지는 필수입니다.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("주문 항목이 비어 있습니다.");
        }
        this.userId = userId;
        this.recipientName = requireText(address.recipientName(), "받는 분");
        this.phone = requireText(address.phone(), "연락처");
        this.postalCode = requireText(address.postalCode(), "우편번호");
        this.address1 = requireText(address.address1(), "주소");
        this.address2 = address.address2();
        this.items = new ArrayList<>(items);
        this.totalAmount = this.items.stream().mapToLong(OrderItem::getLineTotal).sum();
        this.status = OrderStatus.CREATED;
    }

    /** CREATED -> PAID. 종료 상태에서는 거부(IllegalStateException) — 호출자가 사전 검사한다. */
    public void markPaid() {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("CREATED 상태에서만 결제 완료 처리할 수 있습니다. 현재: " + this.status);
        }
        this.status = OrderStatus.PAID;
    }

    public void assignCreationTime(Instant now) {
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void assignUpdateTime(Instant now) {
        this.updatedAt = now;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "은(는) 필수입니다.");
        }
        return value;
    }
}
