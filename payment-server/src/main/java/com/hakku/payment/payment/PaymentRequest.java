package com.hakku.payment.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 결제 요청 바디. {@code userId} 는 인증 주체(JWT subject)에서 오므로 바디에 포함하지 않는다(서비스에 별도 인자로 전달).
 * {@code idempotencyKey} 는 클라이언트가 생성해 동일 요청 재시도를 식별한다. 경계에서 입력을 검증한다(돈을 다루므로 엄격히).
 */
public record PaymentRequest(
        @NotBlank String referenceType,
        @NotBlank String referenceId,
        @Positive long amount,
        @NotBlank String currency,
        @NotBlank String idempotencyKey) {
}
