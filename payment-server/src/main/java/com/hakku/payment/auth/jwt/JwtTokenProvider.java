package com.hakku.payment.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

/**
 * HS256 JWT <b>검증 전용</b> 프로바이더.
 *
 * <p>main-server 가 발급한 액세스 토큰을 공유 비밀키({@code jwt.secret})로 검증하고 subject(=회원 id)를
 * 추출한다. 결제 서버는 토큰을 발급하지 않으므로 생성 기능을 두지 않는다(YAGNI). {@code role} 등 추가
 * 클레임은 결제 인가에 쓰지 않으므로 무시한다 — 결제는 "토큰 주체가 곧 결제 주체"라는 소유 기반 인가다.
 */
public class JwtTokenProvider {

    private final SecretKey key;

    public JwtTokenProvider(String base64Secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
    }

    /** 서명/만료가 유효하면 true. 위조·만료·형식오류 토큰은 모두 false 로 흡수한다. */
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

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
