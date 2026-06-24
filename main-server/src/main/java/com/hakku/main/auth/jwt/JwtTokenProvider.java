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
 * HS256 기반 JWT 액세스/리프레시 토큰 발급·검증 (Mattermost OAuth 제외, JWT-only 인증).
 *
 * <p>토큰 subject = 회원 id, {@code role} 클레임 = {@link Role} 이름, {@code type} 클레임 = access|refresh.
 * 리프레시 토큰은 stateless 다 — 별도 저장 없이 서명·만료·{@code type=refresh} 로만 신뢰한다. 재발급 시
 * DB 조회를 피하려고 {@code role} 을 함께 담는다(역할 변경은 다음 로그인 때 반영).
 */
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String TYPE_CLAIM = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";
    private static final long MILLIS_PER_SECOND = 1000L;

    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtTokenProvider(String base64Secret, long accessTtlSeconds, long refreshTtlSeconds) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public String createAccessToken(String subject, Role role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTtlSeconds * MILLIS_PER_SECOND);
        return createToken(subject, role, now, expiration);
    }

    /** 발급/만료 시각을 직접 지정해 액세스 토큰을 생성한다 (테스트에서 만료 토큰 생성에 사용). */
    String createToken(String subject, Role role, Date issuedAt, Date expiration) {
        return Jwts.builder()
                .subject(subject)
                .claim(ROLE_CLAIM, role.name())
                .claim(TYPE_CLAIM, TYPE_ACCESS)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /** 24시간(설정값) 만료의 리프레시 토큰. {@code type=refresh} + subject·role 을 담는다. */
    public String createRefreshToken(String subject, Role role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTtlSeconds * MILLIS_PER_SECOND);
        return Jwts.builder()
                .subject(subject)
                .claim(ROLE_CLAIM, role.name())
                .claim(TYPE_CLAIM, TYPE_REFRESH)
                .issuedAt(now)
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

    /**
     * 서명·만료가 유효하고 {@code type=refresh} 일 때만 true. 액세스 토큰을 리프레시로 쓰는 것을 막는다.
     * 판정은 {@link #isRefreshToken(String)} 과 동일하므로 위임한다(중복 제거 → 한쪽만 바뀌어 어긋나는 것 방지).
     */
    public boolean validateRefresh(String token) {
        return isRefreshToken(token);
    }

    /** 토큰이 유효 서명·미만료이며 {@code type=refresh} 인지(파싱 실패 시 false). 인증 필터의 리프레시 토큰 거부에도 사용. */
    public boolean isRefreshToken(String token) {
        try {
            return TYPE_REFRESH.equals(parseClaims(token).get(TYPE_CLAIM, String.class));
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
