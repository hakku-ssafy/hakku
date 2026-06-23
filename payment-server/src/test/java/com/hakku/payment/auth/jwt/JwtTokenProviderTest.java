package com.hakku.payment.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 검증 전용 {@link JwtTokenProvider} 단위 테스트. 결제 서버는 토큰을 발급하지 않고 main-server 가 발급한
 * 토큰을 공유 비밀키로 검증만 한다. 따라서 정상 토큰 수용(+subject 추출)과 위조/만료 토큰 거부가 보안 핵심이다.
 */
class JwtTokenProviderTest {

    // HS256 은 최소 32바이트 키가 필요. 아래는 40바이트 비밀키의 base64(테스트 프로파일과 동일).
    private static final String SECRET =
            "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OQ==";
    private static final String OTHER_SECRET = Base64.getEncoder().encodeToString(
            "another-shared-secret-key-of-sufficient-length!!".getBytes(StandardCharsets.UTF_8));

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET);

    @Test
    @DisplayName("accepts a token signed with the shared secret and returns its subject (userId)")
    void validatesAndReadsSubject() {
        String token = TestTokenFactory.token(SECRET, "42");

        assertTrue(provider.validate(token));
        assertEquals("42", provider.getSubject(token));
    }

    @Test
    @DisplayName("rejects a malformed token")
    void rejectsGarbage() {
        assertFalse(provider.validate("not-a-jwt"));
    }

    @Test
    @DisplayName("rejects a token forged with a different secret")
    void rejectsWrongSecret() {
        String forged = TestTokenFactory.token(OTHER_SECRET, "42");

        assertFalse(provider.validate(forged));
    }

    @Test
    @DisplayName("rejects an expired token")
    void rejectsExpired() {
        String expired = TestTokenFactory.token(SECRET, "42", -1_000L);

        assertFalse(provider.validate(expired));
    }
}
