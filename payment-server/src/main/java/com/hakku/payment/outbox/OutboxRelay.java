package com.hakku.payment.outbox;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 트랜잭셔널 아웃박스 릴레이(워커). PENDING 아웃박스 레코드를 폴링해 Kafka 로 발행한다.
 *
 * <p>요청 경로가 아닌 백그라운드 작업이므로 지연이 아니라 신뢰성을 우선한다(전용 producer 설정: acks=all, 멱등 producer).
 * 브로커 ack 를 확인한 <b>뒤에만</b> SENT 로 전이하므로 전송은 at-least-once 이며 소비자는 멱등해야 한다.
 *
 * <p><b>커넥션 점유 주의:</b> Kafka 발행은 블로킹 네트워크 I/O 다. 이를 하나의 긴 트랜잭션으로 감싸면 그 시간 동안
 * DB 커넥션을 붙잡아 풀이 고갈된다. 따라서 발행은 트랜잭션 밖에서 수행하고, 발행에 성공한 레코드만
 * {@link OutboxSentMarker}(짧은 독립 트랜잭션)로 SENT 처리한다.
 *
 * <p><b>poison 격리(M-2):</b> 발행이 실패하면 {@link OutboxFailureMarker} 로 retryCount 를 늘린다. 일시 실패면
 * 순서 보존을 위해 배치를 중단해 다음 폴링에서 재시도하지만, 한계({@link OutboxEvent#MAX_PUBLISH_RETRIES})까지
 * 반복 실패한 레코드는 DEAD 로 격리되어 다음 이벤트로 진행한다 — poison 1건이 뒤 이벤트를 영구히 막지 않게 한다.
 *
 * <p>스케줄 트리거는 {@code OutboxRelayScheduler} 가 담당하며, 테스트는 {@link #publishPending()} 을 직접 호출한다.
 *
 * <p>TODO(후속): 다중 릴레이 인스턴스의 중복 발행 최소화를 위한 SELECT ... FOR UPDATE SKIP LOCKED + 'CLAIMED'
 * 상태 전이(Postgres 전용, Testcontainers 검증 필요). 현재는 at-least-once + 멱등 소비자로 중복을 흡수한다.
 */
@Component
public class OutboxRelay {

    /** 발행 1건의 ack 대기 상한. producer 의 delivery timeout(8s)보다 길게 두어, .get 만료 전에 producer 가 결말을 확정하게 한다. */
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxSentMarker outboxSentMarker;
    private final OutboxFailureMarker outboxFailureMarker;

    public OutboxRelay(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
                       OutboxSentMarker outboxSentMarker, OutboxFailureMarker outboxFailureMarker) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.outboxSentMarker = outboxSentMarker;
        this.outboxFailureMarker = outboxFailureMarker;
    }

    /**
     * 발행 대기(PENDING) 레코드를 생성순으로 조회해 Kafka 로 발행하고, ack 후 SENT 로 전이한다.
     * 발행은 트랜잭션 밖에서 수행하므로 네트워크 I/O 동안 DB 커넥션을 점유하지 않는다.
     *
     * @return 이번 폴링에서 발행 완료한 건수
     */
    public int publishPending() {
        List<OutboxEvent> pending =
                outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        int published = 0;
        for (OutboxEvent event : pending) {
            try {
                kafkaTemplate.send(event.getEventType(), String.valueOf(event.getAggregateId()),
                                event.getPayload())
                        .get(SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // 발행 실패: 재시도 횟수를 기록한다. 한계 도달(DEAD 격리)이면 다음 이벤트로 진행해 poison 이 큐를
                // 막지 않게 하고, 일시 실패면 순서 보존을 위해 배치를 중단해 다음 폴링에서 재시도한다.
                boolean deadLettered = outboxFailureMarker.recordFailure(event.getId());
                if (deadLettered) {
                    continue;
                }
                break;
            }
            outboxSentMarker.markSent(event.getId());
            published++;
        }
        return published;
    }
}
