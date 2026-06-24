package com.hakku.main.order;

/**
 * payment-server 가 {@code payment.approved} 토픽으로 발행하는 결제 승인 이벤트(main-server 소비용 DTO).
 * payment-server 의 PaymentEvent JSON 과 필드명이 일치한다. 결제는 도메인 비종속이므로,
 * referenceType="ORDER" 인 건만 주문 결제 완료로 해석한다(referenceId = 주문 id).
 */
public record PaymentApprovedEvent(
        Long paymentId,
        Long userId,
        String referenceType,
        String referenceId,
        long amount,
        String currency,
        String status,
        String providerTxId,
        long occurredAt) {
}
