package com.hakku.payment.payment.toss;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 토스 successUrl 리다이렉트 후 프론트가 보내는 결제 승인 요청. 세 값 모두 successUrl 쿼리 파라미터에서 온다
 * ({@code paymentKey}, {@code orderId}, {@code amount}).
 */
public record TossConfirmRequest(
        @NotBlank String paymentKey,
        @NotBlank String orderId,
        @Positive long amount) {
}
