package com.hakku.payment.outbox;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아웃박스 레코드를 SENT 로 전이하는 짧은 독립 트랜잭션 경계.
 *
 * <p>릴레이는 Kafka 발행(블로킹 네트워크 I/O)을 트랜잭션 <b>밖</b>에서 수행한 뒤, 발행에 성공한 레코드만 이 메서드로
 * 짧게 커밋한다. 이렇게 분리해야 네트워크 I/O 동안 DB 커넥션을 점유하지 않아 커넥션 풀 고갈을 막을 수 있다.
 * 별도 빈으로 둔 이유는, 같은 빈 내부 호출(self-invocation)로는 {@code @Transactional} 프록시가 적용되지 않기 때문이다.
 */
@Component
public class OutboxSentMarker {

    private final OutboxRepository outboxRepository;

    public OutboxSentMarker(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    /** 발행 성공한 레코드를 PENDING -&gt; SENT 로 전이한다(짧은 독립 트랜잭션, dirty checking 으로 flush). */
    @Transactional
    public void markSent(Long outboxId) {
        outboxRepository.findById(outboxId).ifPresent(OutboxEvent::markSent);
    }
}
