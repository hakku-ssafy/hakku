package com.hakku.payment.gateway;

/**
 * PG 과금 요청 값 객체. JPA 엔티티 대신 값으로 전달해, 게이트웨이 포트가 영속성 클래스에 의존하지 않게 한다.
 */
public record GatewayChargeRequest(
        long amount,
        String currency,
        String referenceType,
        String referenceId,
        String idempotencyKey) {
}
