package com.hakku.main.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 리프레시 토큰 httpOnly 쿠키 빌더. JS 가 읽을 수 없어 XSS 토큰 탈취를 막고, 프론트·API 가 nginx 뒤
 * same-origin 이라 별도 CORS 설정 없이 동작한다. SameSite=Strict 로 CSRF 도 방어한다.
 *
 * <p>Path 를 {@code /api/auth} 로 좁혀 다른 API 요청에는 실리지 않게 한다(/refresh·/logout 에서만 사용).
 */
@Component
public class RefreshTokenCookie {

    public static final String NAME = "refreshToken";
    private static final String PATH = "/api/auth";
    private static final String SAME_SITE = "Strict";

    private final long maxAgeSeconds;
    private final boolean secure;

    public RefreshTokenCookie(
            @Value("${jwt.refresh-ttl-seconds}") long maxAgeSeconds,
            @Value("${auth.refresh-cookie.secure}") boolean secure) {
        this.maxAgeSeconds = maxAgeSeconds;
        this.secure = secure;
    }

    /** 발급된 리프레시 토큰을 담은 만료 24시간(설정값) 쿠키. */
    public ResponseCookie create(String refreshToken) {
        return base(refreshToken, maxAgeSeconds).build();
    }

    /** 즉시 만료(Max-Age=0) 시키는 로그아웃용 쿠키. */
    public ResponseCookie clear() {
        return base("", 0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value, long maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(secure)
                .path(PATH)
                .sameSite(SAME_SITE)
                .maxAge(maxAge);
    }
}
