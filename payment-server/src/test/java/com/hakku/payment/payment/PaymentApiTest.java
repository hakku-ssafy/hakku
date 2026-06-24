package com.hakku.payment.payment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hakku.payment.auth.jwt.TestTokenFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 결제 API 웹 계층 테스트: 보안(JWT) + 컨트롤러 + 예외 매핑(409/400) + JSON 직렬화를 한 번에 검증한다.
 * 인증 주체(토큰 subject)가 결제 주체이며 userId 는 바디로 받지 않는다(소유 기반 인가).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentApiTest {

    @Autowired
    private MockMvc mvc;

    @Value("${jwt.secret}")
    private String secret;

    private String bearer(String userId) {
        return "Bearer " + TestTokenFactory.token(secret, userId);
    }

    private String body(String idempotencyKey, long amount) {
        return """
            {"referenceType":"CART","referenceId":"cart-42","amount":%d,"currency":"KRW","idempotencyKey":"%s"}
            """.formatted(amount, idempotencyKey);
    }

    @Test
    @DisplayName("POST /api/payments without a token → 401")
    void rejectsUnauthenticated() throws Exception {
        mvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("idem-anon", 15000)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a validly-signed token with a non-numeric subject is rejected → 401 (not 500)")
    void rejectsNonNumericSubject() throws Exception {
        mvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + TestTokenFactory.token(secret, "not-a-number"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("idem-nonnum", 15000)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/payments with a valid token → 201 + approved payment owned by the token subject")
    void createsPaymentForAuthenticatedUser() throws Exception {
        mvc.perform(post("/api/payments")
                        .header("Authorization", bearer("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("idem-ok", 15000)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.amount").value(15000));
    }

    @Test
    @DisplayName("reusing an idempotency key with a different amount → 409")
    void duplicateKeyDifferentAmountConflicts() throws Exception {
        mvc.perform(post("/api/payments")
                        .header("Authorization", bearer("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("idem-dup", 15000)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/payments")
                        .header("Authorization", bearer("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("idem-dup", 99999)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("invalid body (non-positive amount) → 400")
    void rejectsInvalidBody() throws Exception {
        mvc.perform(post("/api/payments")
                        .header("Authorization", bearer("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("idem-bad", 0)))
                .andExpect(status().isBadRequest());
    }
}
