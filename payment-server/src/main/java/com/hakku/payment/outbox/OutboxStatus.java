package com.hakku.payment.outbox;

/**
 * 아웃박스 레코드의 발행 상태. PENDING 만 비종료 상태이며, 릴레이가 Kafka 발행에 성공하면 SENT 로 전이한다.
 * 발행이 한계(MAX_PUBLISH_RETRIES)까지 반복 실패하면 DEAD 로 격리해(M-2) poison 이벤트가 큐를 막지 않게 한다.
 */
public enum OutboxStatus {
    PENDING,
    SENT,
    DEAD
}
