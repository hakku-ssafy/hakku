package com.hakku.payment.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * H-4: 결제 엔드포인트 레이트리밋. capacity 를 2 로 낮춰 같은 클라이언트(IP)의 3번째 요청이 429 로
 * 거부되는지 검증한다. 레이트리밋 필터는 인증/컨트롤러보다 앞서므로, 서명 없는 웹훅도 한도 초과 시 429.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "payment.rate-limit.enabled=true",
        "payment.rate-limit.capacity=2",
        "payment.rate-limit.refill-per-second=0"
})
class PaymentRateLimitTest {

    @Autowired
    private MockMvc mvc;

    private static final String WEBHOOK_BODY =
            "{\"idempotencyKey\":\"rl-1\",\"providerTxId\":\"tx\",\"outcome\":\"APPROVED\"}";

    @Test
    @DisplayName("capacity(2) 초과 시 같은 클라이언트의 다음 요청은 429")
    void exceedingLimitReturns429() throws Exception {
        // 앞 2건: 레이트리밋 통과 → 서명 없는 웹훅이라 401.
        mvc.perform(post("/api/payments/webhooks/pg")
                        .contentType(MediaType.APPLICATION_JSON).content(WEBHOOK_BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/payments/webhooks/pg")
                        .contentType(MediaType.APPLICATION_JSON).content(WEBHOOK_BODY))
                .andExpect(status().isUnauthorized());
        // 3번째: 토큰 소진 → 429 (컨트롤러/보안 도달 전 차단).
        mvc.perform(post("/api/payments/webhooks/pg")
                        .contentType(MediaType.APPLICATION_JSON).content(WEBHOOK_BODY))
                .andExpect(status().isTooManyRequests());
    }
}
