package com.hakku.payment.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도메인 이벤트를 JSON 으로 직렬화해 PENDING 아웃박스 레코드로 저장한다.
 *
 * <p>반드시 호출자의 {@code @Transactional} 경계 안에서 실행되어야 한다. 그래야 비즈니스 상태 변경(예: 결제 승인)과
 * 아웃박스 INSERT 가 하나의 트랜잭션으로 원자적으로 커밋되어 이중 쓰기 문제(상태는 바뀌었는데 이벤트는 유실)를 막는다.
 */
@Component
public class OutboxRecorder {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxRecorder(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 페이로드를 JSON 으로 직렬화해 PENDING 아웃박스 레코드를 저장한다.
     *
     * @param aggregateType 애그리거트 종류(예: "PAYMENT")
     * @param aggregateId   애그리거트 식별자(결제 id) — Kafka 메시지 키로 사용된다
     * @param eventType     도메인 이벤트 종류이자 Kafka 토픽명(예: "payment.approved")
     * @param payload       직렬화 대상 이벤트 객체
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent record(String aggregateType, Long aggregateId, String eventType, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("아웃박스 이벤트 직렬화 실패: " + eventType, e);
        }
        return outboxRepository.save(new OutboxEvent(aggregateType, aggregateId, eventType, json));
    }
}
