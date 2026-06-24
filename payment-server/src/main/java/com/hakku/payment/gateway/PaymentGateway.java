package com.hakku.payment.gateway;

/**
 * 결제 대행사(PG) 추상화 포트. Mock 과 실 PG 구현이 이 인터페이스를 구현하며,
 * 서비스 계층은 구체 PG 가 아닌 이 포트에만 의존한다.
 */
public interface PaymentGateway {

    /** 이 게이트웨이의 provider 식별자(예: "MOCK"). 결제에 기록된다. */
    String providerName();

    /**
     * 과금을 시도하고 승인/실패 결과를 반환한다.
     *
     * <p><b>계약(필수):</b> 구현체는 {@link GatewayChargeRequest#idempotencyKey()} 기준으로 멱등해야 한다.
     * 동일 키로 재요청이 오면 <b>다시 과금하지 말고</b> 최초 결과를 그대로 반환해야 한다.
     * 이는 크래시-과금 후 재시도(트랜잭션 롤백으로 PENDING 행이 사라진 경우)나 PG 타임아웃 재시도에서의
     * 이중과금을 막는 마지막 방어선이다. DB UNIQUE 는 같은 프로세스 내 동시 레이스만 막을 뿐이다.
     */
    GatewayResult charge(GatewayChargeRequest request);
}
