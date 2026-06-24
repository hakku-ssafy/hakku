package com.hakku.main.order.domain;

/** 배송지 입력 값 객체. 주문 생성 시 받아 Order 에 스냅샷한다. */
public record ShippingAddress(
        String recipientName,
        String phone,
        String postalCode,
        String address1,
        String address2) {
}
