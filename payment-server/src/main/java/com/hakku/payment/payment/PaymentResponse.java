package com.hakku.payment.payment;

import com.hakku.payment.payment.domain.Payment;
import com.hakku.payment.payment.domain.PaymentStatus;

/**
 * 결제 응답 DTO. 엔티티를 직접 노출하지 않고 서비스/컨트롤러 경계에서 매핑한다.
 */
public record PaymentResponse(
        Long id,
        Long userId,
        String referenceType,
        String referenceId,
        long amount,
        String currency,
        PaymentStatus status,
        String provider,
        String providerTxId) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                payment.getReferenceType(),
                payment.getReferenceId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getProvider(),
                payment.getProviderTxId());
    }
}
