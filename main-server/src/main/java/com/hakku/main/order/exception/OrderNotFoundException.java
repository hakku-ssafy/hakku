package com.hakku.main.order.exception;

/** 본인 주문이 아니거나 존재하지 않을 때 발생(존재 노출 방지). HTTP 404 로 매핑된다. */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId) {
        super("주문을 찾을 수 없습니다: id=" + orderId);
    }
}
