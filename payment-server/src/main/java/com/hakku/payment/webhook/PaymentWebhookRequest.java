package com.hakku.payment.webhook;

/**
 * PG 결제 웹훅 본문. {@code idempotencyKey} 로 결제를 식별하고 {@code outcome}("APPROVED"/"FAILED")으로 최종 상태를 전달한다.
 * 서명 검증 후 raw 본문을 이 레코드로 파싱한다.
 */
public record PaymentWebhookRequest(
        String idempotencyKey,
        String providerTxId,
        String outcome,
        String failureReason) {
}
