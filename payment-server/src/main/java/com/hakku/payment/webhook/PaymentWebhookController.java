package com.hakku.payment.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PG 결제 웹훅 수신 엔드포인트. JWT 가 아니라 <b>HMAC 서명</b>으로 인증한다(SecurityConfig 에서 permitAll).
 *
 * <p>처리 순서: (1) raw 본문 바이트로 서명 검증 — 실패 시 401, (2) 파싱 실패 시 400, (3) 정산은 멱등이라 PG 재전송에 안전,
 * (4) 알 수 없는 결제 참조면 {@code PaymentNotFoundException} → 404. 정상 처리는 200.
 */
@RestController
@RequestMapping("/api/payments/webhooks")
public class PaymentWebhookController {

    private static final String SIGNATURE_HEADER = "X-Signature";

    private final WebhookSignatureVerifier signatureVerifier;
    private final PaymentWebhookService webhookService;
    private final ObjectMapper objectMapper;

    public PaymentWebhookController(WebhookSignatureVerifier signatureVerifier,
                                    PaymentWebhookService webhookService, ObjectMapper objectMapper) {
        this.signatureVerifier = signatureVerifier;
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/pg")
    public ResponseEntity<Void> receive(
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            @RequestBody String rawBody) {
        if (!signatureVerifier.isValid(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        PaymentWebhookRequest request;
        try {
            request = objectMapper.readValue(rawBody, PaymentWebhookRequest.class);
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().build();
        }
        webhookService.apply(request);
        return ResponseEntity.ok().build();
    }
}
