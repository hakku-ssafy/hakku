package com.hakku.payment.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hakku.payment.payment.PaymentRepository;
import com.hakku.payment.payment.domain.Payment;
import com.hakku.payment.payment.domain.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * PG 웹훅 웹 계층 테스트: 서명 인증(401) + 정산(200) + 멱등 재전송(200) + 미지 결제(404).
 * 웹훅은 JWT 가 아니라 HMAC 서명으로 인증되며, 동기 경로와 같은 PaymentSettler 로 멱등 정산한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentWebhookApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${payment.webhook.secret}")
    private String webhookSecret;

    private Payment seedPending(String idempotencyKey) {
        return paymentRepository.saveAndFlush(
                new Payment(7L, "CART", "cart-1", 15000L, "KRW", "MOCK", idempotencyKey));
    }

    private String body(String idempotencyKey, String outcome) {
        return """
            {"idempotencyKey":"%s","providerTxId":"PG-tx-1","outcome":"%s","failureReason":null}
            """.formatted(idempotencyKey, outcome);
    }

    @Test
    @DisplayName("a correctly-signed APPROVED webhook settles the PENDING payment → 200 + APPROVED")
    void approvedWebhookSettlesPending() throws Exception {
        seedPending("wh-ok");
        String payload = body("wh-ok", "APPROVED");

        mvc.perform(post("/api/payments/webhooks/pg")
                        .header("X-Signature", WebhookTestSigner.sign(webhookSecret, payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        Payment settled = paymentRepository.findByIdempotencyKey("wh-ok").orElseThrow();
        assertEquals(PaymentStatus.APPROVED, settled.getStatus());
        assertEquals("PG-tx-1", settled.getProviderTxId());
    }

    @Test
    @DisplayName("a forged signature is rejected → 401 (and the payment stays PENDING)")
    void forgedSignatureRejected() throws Exception {
        seedPending("wh-forge");
        String payload = body("wh-forge", "APPROVED");

        mvc.perform(post("/api/payments/webhooks/pg")
                        .header("X-Signature", "deadbeef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        Payment untouched = paymentRepository.findByIdempotencyKey("wh-forge").orElseThrow();
        assertEquals(PaymentStatus.PENDING, untouched.getStatus());
    }

    @Test
    @DisplayName("a missing signature header is rejected → 401")
    void missingSignatureRejected() throws Exception {
        String payload = body("wh-nosig", "APPROVED");

        mvc.perform(post("/api/payments/webhooks/pg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a webhook for an unknown payment reference → 404")
    void unknownPaymentReturns404() throws Exception {
        String payload = body("wh-unknown", "APPROVED");

        mvc.perform(post("/api/payments/webhooks/pg")
                        .header("X-Signature", WebhookTestSigner.sign(webhookSecret, payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a signed webhook with an unknown outcome → 400 (payment stays PENDING, not silently FAILED)")
    void unknownOutcomeRejected() throws Exception {
        seedPending("wh-weird");
        String payload = body("wh-weird", "REVERSED");

        mvc.perform(post("/api/payments/webhooks/pg")
                        .header("X-Signature", WebhookTestSigner.sign(webhookSecret, payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        Payment untouched = paymentRepository.findByIdempotencyKey("wh-weird").orElseThrow();
        assertEquals(PaymentStatus.PENDING, untouched.getStatus());
    }

    @Test
    @DisplayName("a resent (duplicate) webhook is idempotent → 200, payment stays APPROVED once")
    void idempotentReplay() throws Exception {
        seedPending("wh-replay");
        String payload = body("wh-replay", "APPROVED");
        String signature = WebhookTestSigner.sign(webhookSecret, payload);

        mvc.perform(post("/api/payments/webhooks/pg")
                        .header("X-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
        mvc.perform(post("/api/payments/webhooks/pg")
                        .header("X-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        Payment settled = paymentRepository.findByIdempotencyKey("wh-replay").orElseThrow();
        assertEquals(PaymentStatus.APPROVED, settled.getStatus());
    }
}
