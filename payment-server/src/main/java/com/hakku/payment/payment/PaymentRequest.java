package com.hakku.payment.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 결제 요청 바디. {@code userId} 는 인증 주체(JWT subject)에서 오므로 바디에 포함하지 않는다(서비스에 별도 인자로 전달).
 * {@code idempotencyKey} 는 클라이언트가 생성해 동일 요청 재시도를 식별한다. 경계에서 입력을 검증한다(돈을 다루므로 엄격히).
 *
 * <p>M-5: idempotencyKey 는 길이(≤128)·문자셋([A-Za-z0-9-_])을 검증한다. 미검증 시 255자 초과나
 * 이상 문자가 DB 까지 내려가 제약 위반 500 으로 누수된다 — 경계에서 400 으로 막는다.
 */
public record PaymentRequest(
        @NotBlank String referenceType,
        @NotBlank String referenceId,
        @Positive long amount,
        @NotBlank String currency,
        @NotBlank @Size(max = 128) @Pattern(regexp = "[A-Za-z0-9\\-_]+") String idempotencyKey) {
}
