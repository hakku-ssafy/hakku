package com.hakku.payment.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;

/**
 * 테스트 전용 토큰 발급기. main-server 가 발급하는 형태(subject=userId, {@code role} 클레임)의 액세스 토큰을
 * 동일한 공유 비밀키로 만들어, 검증 전용 {@link JwtTokenProvider} 가 이를 받아들이는지(상호운용)와
 * 위조/만료 토큰을 거부하는지를 확인하는 데 쓴다.
 */
public final class TestTokenFactory {

    private TestTokenFactory() {
    }

    /** 기본 1시간 만료 토큰. */
    public static String token(String base64Secret, String subject) {
        return token(base64Secret, subject, 3_600_000L);
    }

    /** 만료까지 {@code ttlMillis} 인 토큰(음수면 이미 만료된 토큰)을 생성한다. */
    public static String token(String base64Secret, String subject, long ttlMillis) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("role", "NORMAL")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
    }

    /** main-server 가 발급하는 리프레시 토큰 형태({@code type=refresh}). 액세스 토큰으로 거부되는지 검증용. */
    public static String refreshToken(String base64Secret, String subject) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("role", "NORMAL")
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 86_400_000L))
                .signWith(key)
                .compact();
    }
}
