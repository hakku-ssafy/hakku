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
                // M-3: 발행(relay) 시각이 아니라 결제 엔티티의 전이 시각을 쓴다. 영속 엔티티는 항상 채워져 있으나,
                // 미영속(테스트 등) 엔티티의 null 로 인한 NPE 를 막기 위해 now 로 폴백한다.
                payment.getUpdatedAt() != null
                        ? payment.getUpdatedAt().toEpochMilli()
                        : Instant.now().toEpochMilli());
    }
}
