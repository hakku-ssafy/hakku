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
 * <p>발행 실패 시 해당 레코드를 PENDING 으로 두고 배치를 중단해 애그리거트별 순서를 보존한다(다음 폴링에서 재시도).
 * 스케줄 트리거는 {@link OutboxRelayScheduler} 가 담당하며, 테스트는 {@link #publishPending()} 을 직접 호출한다.
 *
 * <p>TODO(차기 Task): 영구 발행 불가(poison) 메시지를 위한 retryCount/DEAD 상태와 데드레터 경로. 현재는 토픽명을
 * 상수로 관리(오타 방지)하고 auto-create 가 켜져 있어 위험은 낮지만, 항구적 실패는 아웃박스 진행을 막을 수 있다.
 */
@Component
public class OutboxRelay {

    /** 발행 1건의 ack 대기 상한. producer 의 delivery timeout(8s)보다 길게 두어, .get 만료 전에 producer 가 결말을 확정하게 한다. */
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxSentMarker outboxSentMarker;

    public OutboxRelay(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
                       OutboxSentMarker outboxSentMarker) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.outboxSentMarker = outboxSentMarker;
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
                // 발행 실패(타임아웃/브로커 오류): 다음 폴링에서 재시도. 순서 보존을 위해 배치 중단.
                break;
            }
            outboxSentMarker.markSent(event.getId());
            published++;
        }
        return published;
    }
}
