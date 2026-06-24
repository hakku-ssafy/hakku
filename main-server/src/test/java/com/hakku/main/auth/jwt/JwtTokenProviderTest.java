package com.hakku.main.auth.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hakku.main.user.domain.Role;
import java.util.Date;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    // 40 bytes (>= 256-bit) base64 secret — HS256 에 충분
    private static final String SECRET = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OQ==";
    private static final long TTL_SECONDS = 3600;
    private static final long REFRESH_TTL_SECONDS = 86_400;

    private final JwtTokenProvider provider =
            new JwtTokenProvider(SECRET, TTL_SECONDS, REFRESH_TTL_SECONDS);

    @Test
    @DisplayName("발급한 토큰에서 subject(회원 id)와 role 을 복원한다")
    void createAndParse() {
        String token = provider.createAccessToken("42", Role.SELLER);

        assertTrue(provider.validate(token));
        assertEquals("42", provider.getSubject(token));
        assertEquals(Role.SELLER, provider.getRole(token));
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 invalid")
    void garbageIsInvalid() {
        assertFalse(provider.validate("not.a.jwt"));
        assertFalse(provider.validate(""));
    }

    @Test
    @DisplayName("변조된 토큰은 invalid")
    void tamperedIsInvalid() {
        String token = provider.createAccessToken("42", Role.NORMAL);

        assertFalse(provider.validate(token + "tampered"));
    }

    @Test
    @DisplayName("다른 시크릿으로는 검증 실패 (서명 불일치)")
    void wrongSecretIsInvalid() {
        String token = provider.createAccessToken("42", Role.NORMAL);
        var other = new JwtTokenProvider(
                "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg5", TTL_SECONDS, REFRESH_TTL_SECONDS);

        assertFalse(other.validate(token));
    }

    @Test
    @DisplayName("만료된 토큰은 invalid")
    void expiredIsInvalid() {
        // 1970년에 발급/만료된 토큰을 강제로 생성 — 시계에 의존하지 않는 결정적 테스트
        String expired = provider.createToken("42", Role.NORMAL, new Date(0L), new Date(1000L));

        assertFalse(provider.validate(expired));
    }

    @Test
    @DisplayName("리프레시 토큰은 validateRefresh 를 통과하고 subject·role 을 복원한다")
    void refreshTokenValidatesAndCarriesClaims() {
        String refresh = provider.createRefreshToken("42", Role.SELLER);

        assertTrue(provider.validateRefresh(refresh));
        assertEquals("42", provider.getSubject(refresh));
        assertEquals(Role.SELLER, provider.getRole(refresh));
    }

    @Test
    @DisplayName("액세스 토큰은 validateRefresh 에서 거부된다(type 불일치)")
    void accessTokenIsNotAcceptedAsRefresh() {
        String access = provider.createAccessToken("42", Role.NORMAL);

        assertFalse(provider.validateRefresh(access));
    }

    @Test
    @DisplayName("isRefreshToken: 리프레시 토큰만 true, 액세스 토큰은 false")
    void isRefreshTokenDistinguishesType() {
        String refresh = provider.createRefreshToken("42", Role.NORMAL);
        String access = provider.createAccessToken("42", Role.NORMAL);

        assertTrue(provider.isRefreshToken(refresh));
        assertFalse(provider.isRefreshToken(access));
    }

    @Test
    @DisplayName("만료된 리프레시 토큰은 validateRefresh 에서 거부된다(refresh TTL 이 실제로 적용됨)")
    void expiredRefreshIsInvalid() {
        // refresh TTL 0 → 발급 즉시 만료. refreshTtlSeconds 가 실제로 만료에 반영되는지 검증.
        var shortLived = new JwtTokenProvider(SECRET, TTL_SECONDS, 0L);
        String refresh = shortLived.createRefreshToken("42", Role.NORMAL);

        assertFalse(shortLived.validateRefresh(refresh));
    }
}
