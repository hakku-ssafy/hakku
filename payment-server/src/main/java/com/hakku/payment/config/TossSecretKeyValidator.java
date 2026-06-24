package com.hakku.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 부팅 시 토스 시크릿키를 검증해 잘못된 키로 기동하는 것을 막는다(fail-fast).
 *
 * <p>키가 비어있거나 샌드박스(test_) 키면 운영에서 실결제가 조용히 실패하므로, 생성자에서
 * {@link IllegalStateException} 을 던져 컨텍스트 로딩 자체를 실패시킨다. 단위 테스트나 샌드박스
 * 통합 테스트는 {@code payment.toss.secret-key-validation-enabled=false} 로 검증을 끈다.
 */
@Component
public class TossSecretKeyValidator {

    private static final String SANDBOX_PREFIX = "test_";

    public TossSecretKeyValidator(
            @Value("${payment.toss.secret-key}") String secretKey,
            @Value("${payment.toss.secret-key-validation-enabled:true}") boolean enabled) {
        if (enabled) {
            validate(secretKey);
        }
    }

    /** 빈/공백 또는 샌드박스(test_) 키를 거부한다(앞뒤 공백은 무시). */
    static void validate(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException(
                    "payment.toss.secret-key (TOSS_SECRET_KEY) must be set");
        }
        if (secretKey.strip().startsWith(SANDBOX_PREFIX)) {
            throw new IllegalStateException(
                    "payment.toss.secret-key must not be a sandbox(test_) key — set a live TOSS_SECRET_KEY");
        }
    }
}
