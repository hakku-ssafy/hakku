package com.hakku.main.auth.jwt;

import com.hakku.main.user.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;

/**
 * HS256 기반 JWT 액세스 토큰 발급/검증 (Mattermost OAuth 제외, JWT-only 인증).
 *
 * <p>토큰 subject = 회원 id, {@code role} 클레임 = {@link Role} 이름.
 */
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";
    private static final long MILLIS_PER_SECOND = 1000L;

    private final SecretKey key;
    private final long accessTtlSeconds;

    public JwtTokenProvider(String base64Secret, long accessTtlSeconds) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.accessTtlSeconds = accessTtlSeconds;
    }

    public String createAccessToken(String subject, Role role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTtlSeconds * MILLIS_PER_SECOND);
        return createToken(subject, role, now, expiration);
    }

    /** 발급/만료 시각을 직접 지정해 토큰을 생성한다 (테스트에서 만료 토큰 생성에 사용). */
    String createToken(String subject, Role role, Date issuedAt, Date expiration) {
        return Jwts.builder()
                .subject(subject)
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String getSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public Role getRole(String token) {
        return Role.valueOf(parseClaims(token).get(ROLE_CLAIM, String.class));
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
