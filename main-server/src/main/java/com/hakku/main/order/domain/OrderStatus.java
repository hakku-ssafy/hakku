package com.hakku.main.order.domain;

/** 주문 상태. 결제 전 CREATED, 결제 승인(payment.approved) 시 PAID. */
public enum OrderStatus {
    CREATED,
    PAID,
    CANCELLED
}
