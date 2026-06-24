package com.hakku.payment.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아웃박스 발행 실패를 짧은 독립 트랜잭션으로 기록하는 경계(M-2). {@link OutboxSentMarker} 와 대칭으로,
 * Kafka 발행(트랜잭션 밖 네트워크 I/O)이 실패했을 때 retryCount 를 늘리고 한계 도달 시 DEAD 로 격리한다.
 *
 * <p>별도 빈으로 둔 이유는 self-invocation 으로는 {@code @Transactional} 프록시가 적용되지 않기 때문이다.
 */
@Component
public class OutboxFailureMarker {

    private final OutboxRepository outboxRepository;

    public OutboxFailureMarker(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    /** 발행 실패 1회를 기록한다. 재시도 한계 도달로 DEAD 격리되면 {@code true}. */
    @Transactional
    public boolean recordFailure(Long outboxId) {
        return outboxRepository.findById(outboxId)
                .map(OutboxEvent::recordFailure)
                .orElse(false);
    }
}
