package com.hakku.payment.payment.exception;

/**
 * 정산/조회 대상 결제를 찾을 수 없는 경우(404 성격). 클라이언트(또는 PG 웹훅)에는 일반화 메시지만 노출한다.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
