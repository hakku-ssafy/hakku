package com.hakku.payment.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H-2: TOSS_SECRET_KEY fail-fast. 빈 키나 샌드박스(test_) 키로 부팅하면 운영에서 실결제가
 * 조용히 실패하므로, 부팅 시점에 거부해야 한다.
 */
class TossSecretKeyValidatorTest {

    @Test
    @DisplayName("빈/공백/널 시크릿키는 IllegalStateException")
    void blankRejected() {
        assertThrows(IllegalStateException.class, () -> TossSecretKeyValidator.validate(null));
        assertThrows(IllegalStateException.class, () -> TossSecretKeyValidator.validate(""));
        assertThrows(IllegalStateException.class, () -> TossSecretKeyValidator.validate("   "));
    }

    @Test
    @DisplayName("test_ 접두(샌드박스) 키는 IllegalStateException — 운영 무음 오작동 방지")
    void sandboxPrefixRejected() {
        assertThrows(IllegalStateException.class,
                () -> TossSecretKeyValidator.validate("test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6"));
        // 앞뒤 공백이 있어도 strip 후 판정한다
        assertThrows(IllegalStateException.class,
                () -> TossSecretKeyValidator.validate("  test_gsk_x  "));
    }

    @Test
    @DisplayName("운영(live) 키는 통과")
    void liveKeyPasses() {
        assertDoesNotThrow(() -> TossSecretKeyValidator.validate("live_gsk_realSecretKeyValue"));
    }
}
