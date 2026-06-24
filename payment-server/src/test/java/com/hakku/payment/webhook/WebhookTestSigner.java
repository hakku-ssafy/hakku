package com.hakku.payment.webhook;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 테스트 전용 웹훅 서명 생성기. 검증기와 <b>독립적으로</b> HMAC-SHA256 hex 를 계산해, 검증기가 PG 가 만든 서명을
 * 올바로 받아들이는지 상호 검증한다.
 */
public final class WebhookTestSigner {

    private WebhookTestSigner() {
    }

    public static String sign(String secret, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
