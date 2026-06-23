package com.hakku.payment.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hakku.payment.payment.domain.Payment;
import com.hakku.payment.payment.domain.PaymentStatus;
import java.time.Instant;

/**
 * Kafka 결제 토픽으로 발행되는 이벤트 페이로드. 아웃박스 payload(JSON)로 직렬화되어 그대로 메시지 값이 된다.
 * null 필드는 직렬화에서 제외한다(승인 전 providerTxId 등).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentEvent(
        Long paymentId,
        Long userId,
        String referenceType,
        String referenceId,
        long amount,
        String currency,
        PaymentStatus status,
        String providerTxId,
        long occurredAt) {

    public static PaymentEvent from(Payment payment) {
        return new PaymentEvent(
                payment.getId(),
                payment.getUserId(),
                payment.getReferenceType(),
                payment.getReferenceId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getProviderTxId(),
                Instant.now().toEpochMilli());
    }
}
