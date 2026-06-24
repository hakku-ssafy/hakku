package com.hakku.payment.webhook;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * PG 웹훅 서명 검증기. 웹훅은 JWT 가 아니라 <b>서명이 곧 인증</b>이다 — PG 와 공유한 비밀키로 raw 본문의
 * HMAC-SHA256 을 계산해 헤더값과 <b>상수시간</b> 비교한다(타이밍 공격 방지).
 *
 * <p>반드시 수신한 raw 본문 바이트 그대로 검증해야 한다. 역직렬화 후 재직렬화하면 바이트가 달라져 서명이 어긋난다.
 */
@Component
public class WebhookSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    /** M-6: HMAC-SHA256 키는 최소 32바이트여야 한다. 약한 키로 부팅하면 위조 방어가 약화되므로 fail-fast. */
    private static final int MIN_SECRET_LENGTH = 32;

    private final byte[] secret;

    public WebhookSignatureVerifier(@Value("${payment.webhook.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "payment.webhook.secret 은 최소 " + MIN_SECRET_LENGTH
                            + "바이트여야 합니다 (현재 " + bytes.length + "바이트).");
        }
        this.secret = bytes;
    }

    /** raw 본문에 대한 서명이 유효하면 true. 헤더가 없거나 본문이 null 이면 false. */
    public boolean isValid(String rawBody, String signatureHeader) {
        if (rawBody == null || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        byte[] expected = hmacHex(rawBody).getBytes(StandardCharsets.UTF_8);
        byte[] provided = signatureHeader.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }

    private String hmacHex(String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC 계산 실패", e);
        }
    }
}
