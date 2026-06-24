package com.hakku.payment.payment.toss;

import com.hakku.payment.payment.domain.Payment;

/** confirm 응답. 프론트는 {@code status}(APPROVED/FAILED)로 성공/실패 화면을 분기한다. */
public record TossConfirmResponse(Long paymentId, String status, long amount) {

    public static TossConfirmResponse from(Payment payment) {
        return new TossConfirmResponse(payment.getId(), payment.getStatus().name(), payment.getAmount());
    }
}
