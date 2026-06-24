package com.hakku.payment.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PG 결과 값 객체. 정적 팩터리만으로 생성해 잘못된 조합(승인인데 사유 존재 등)을 표현 불가능하게 만든다.
 */
class GatewayResultTest {

    @Test
    @DisplayName("approved carries a transaction id and no failure reason")
    void approved() {
        var result = GatewayResult.approved("pg_tx_001");

        assertTrue(result.approved());
        assertEquals("pg_tx_001", result.providerTxId());
        assertNull(result.failureReason());
    }

    @Test
    @DisplayName("approved requires a transaction id")
    void approvedRequiresTxId() {
        assertThrows(IllegalArgumentException.class, () -> GatewayResult.approved(" "));
    }

    @Test
    @DisplayName("failed carries a reason and no transaction id")
    void failed() {
        var result = GatewayResult.failed("INSUFFICIENT_FUNDS");

        assertFalse(result.approved());
        assertEquals("INSUFFICIENT_FUNDS", result.failureReason());
        assertNull(result.providerTxId());
    }

    @Test
    @DisplayName("failed requires a reason")
    void failedRequiresReason() {
        assertThrows(IllegalArgumentException.class, () -> GatewayResult.failed(" "));
    }
}
