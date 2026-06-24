package com.hakku.main.order.exception;

/** 빈 장바구니로 주문을 생성하려 할 때 발생. HTTP 400 으로 매핑된다. */
public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("장바구니가 비어 있어 주문할 수 없습니다.");
    }
}
