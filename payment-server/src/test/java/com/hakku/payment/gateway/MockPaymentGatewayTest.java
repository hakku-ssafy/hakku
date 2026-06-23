package com.hakku.payment.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 샌드박스 게이트웨이. 실 PG 연동 전까지 결정론적으로 승인하며, 멱등 키 기반 거래 id 를 발급한다.
 */
class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();

    @Test
    @DisplayName("provider name is MOCK")
    void providerName() {
        assertEquals("MOCK", gateway.providerName());
    }

    @Test
    @DisplayName("charge approves with a deterministic transaction id derived from the idempotency key")
    void chargeApproves() {
        var request = new GatewayChargeRequest(15000L, "KRW", "CART", "cart-42", "idem-1");

        var result = gateway.charge(request);

        assertTrue(result.approved());
        assertEquals("MOCK-idem-1", result.providerTxId());
    }
}
