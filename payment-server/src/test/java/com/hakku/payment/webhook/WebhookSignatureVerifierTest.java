package com.hakku.payment.webhook;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 웹훅 서명 검증기 단위 테스트. 서명이 곧 인증이므로 정상 서명 수용 + 위조/변조/누락 거부가 보안 핵심이다.
 */
class WebhookSignatureVerifierTest {

    private static final String SECRET = "test-webhook-secret";

    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(SECRET);

    @Test
    @DisplayName("accepts a body whose signature matches the shared secret")
    void acceptsValidSignature() {
        String body = "{\"idempotencyKey\":\"k1\",\"outcome\":\"APPROVED\"}";

        assertTrue(verifier.isValid(body, WebhookTestSigner.sign(SECRET, body)));
    }

    @Test
    @DisplayName("rejects a signature computed with a different secret (forged)")
    void rejectsForgedSignature() {
        String body = "{\"idempotencyKey\":\"k1\",\"outcome\":\"APPROVED\"}";

        assertFalse(verifier.isValid(body, WebhookTestSigner.sign("other-secret", body)));
    }

    @Test
    @DisplayName("rejects when the body was tampered after signing")
    void rejectsTamperedBody() {
        String signature = WebhookTestSigner.sign(SECRET, "{\"amount\":1000}");

        assertFalse(verifier.isValid("{\"amount\":999999}", signature));
    }

    @Test
    @DisplayName("rejects a missing or blank signature header")
    void rejectsMissingSignature() {
        String body = "{\"idempotencyKey\":\"k1\"}";

        assertFalse(verifier.isValid(body, null));
        assertFalse(verifier.isValid(body, "   "));
    }
}
