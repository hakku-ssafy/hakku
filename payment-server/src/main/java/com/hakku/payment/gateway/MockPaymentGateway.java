package com.hakku.payment.gateway;

import org.springframework.stereotype.Component;

/**
 * 샌드박스 PG 구현. 실 PG 연동 전까지 항상 승인하며, 멱등 키 기반의 결정론적 거래 id 를 발급한다.
 * 실 PG 어댑터가 추가되면 이 빈을 교체하거나 프로파일로 분리한다.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final String PROVIDER = "MOCK";

    @Override
    public String providerName() {
        return PROVIDER;
    }

    @Override
    public GatewayResult charge(GatewayChargeRequest request) {
        return GatewayResult.approved(PROVIDER + "-" + request.idempotencyKey());
    }
}
