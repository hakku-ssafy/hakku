package com.hakku.payment.payment.toss;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hakku.payment.auth.jwt.TestTokenFactory;
import com.hakku.payment.gateway.GatewayResult;
import com.hakku.payment.gateway.toss.TossPaymentClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 토스 결제 API 웹 계층 테스트(보안 + 컨트롤러 + 예외 매핑). 실 토스 호출은 {@link TossPaymentClient} 를 모킹해 차단한다
 * (샌드박스 단위테스트). prepare → confirm 2단계를 실제 H2 PENDING 으로 이어 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TossPaymentApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String secret;

    @MockitoBean
    private TossPaymentClient tossPaymentClient;

    private String bearer(String userId) {
        return "Bearer " + TestTokenFactory.token(secret, userId);
    }

    private String prepareBody(long amount) {
        return """
            {"referenceType":"PRODUCT","referenceId":"42","amount":%d,"currency":"KRW"}
            """.formatted(amount);
    }

    private String prepareAndGetOrderId(String userId, long amount) throws Exception {
        String json = mvc.perform(post("/api/payments/toss/prepare")
                        .header("Authorization", bearer(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(prepareBody(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("orderId").asText();
    }

    @Test
    @DisplayName("POST /api/payments/toss/prepare 토큰 없으면 401")
    void prepareRequiresAuth() throws Exception {
        mvc.perform(post("/api/payments/toss/prepare")
                        .contentType(MediaType.APPLICATION_JSON).content(prepareBody(15000)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("prepare → 200 + orderId, 이어서 confirm(승인) → 200 APPROVED")
    void prepareThenConfirmApproves() throws Exception {
        String orderId = prepareAndGetOrderId("7", 15000);
        when(tossPaymentClient.confirm(eq("pk_1"), eq(orderId), anyLong()))
                .thenReturn(GatewayResult.approved("pk_1"));

        String confirmBody = """
            {"paymentKey":"pk_1","orderId":"%s","amount":15000}
            """.formatted(orderId);
        mvc.perform(post("/api/payments/toss/confirm")
                        .header("Authorization", bearer("7"))
                        .contentType(MediaType.APPLICATION_JSON).content(confirmBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.amount").value(15000));
    }

    @Test
    @DisplayName("confirm: 저장 금액과 다르면 400 (토스 호출 안 함)")
    void confirmAmountMismatch() throws Exception {
        String orderId = prepareAndGetOrderId("7", 15000);
        String confirmBody = """
            {"paymentKey":"pk_1","orderId":"%s","amount":99999}
            """.formatted(orderId);
        mvc.perform(post("/api/payments/toss/confirm")
                        .header("Authorization", bearer("7"))
                        .contentType(MediaType.APPLICATION_JSON).content(confirmBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("confirm: 미지의 orderId → 404")
    void confirmUnknownOrder() throws Exception {
        String confirmBody = """
            {"paymentKey":"pk_1","orderId":"order_unknown_xyz123","amount":15000}
            """;
        mvc.perform(post("/api/payments/toss/confirm")
                        .header("Authorization", bearer("7"))
                        .contentType(MediaType.APPLICATION_JSON).content(confirmBody))
                .andExpect(status().isNotFound());
    }
}
