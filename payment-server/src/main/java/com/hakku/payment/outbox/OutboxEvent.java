package com.hakku.payment.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 트랜잭셔널 아웃박스 레코드. 결제 상태 변경과 <b>같은 트랜잭션</b>에서 INSERT 되어 이벤트 유실/유령 발행을 막는다
 * (이중 쓰기 문제 해소). 별도의 릴레이가 PENDING 행을 폴링해 Kafka 로 발행한 뒤 {@link #markSent()} 로 SENT 처리한다.
 *
 * <p>at-least-once 보장이므로 소비자는 멱등해야 한다. 발행 시 {@link #payload} JSON 문자열을 그대로 메시지 값으로
 * 보내며(직렬화 원천), {@link #eventType} 을 토픽으로 사용한다.
 */
@Entity
@Table(name = "payment_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 애그리거트 종류(예: "PAYMENT"). 아웃박스를 자기 기술적으로 만들고 재사용 가능하게 한다. */
    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    /** 애그리거트 식별자(결제 id). Kafka 메시지 키로 사용해 애그리거트별 순서를 보장한다. */
    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    /** 도메인 이벤트 종류이자 Kafka 토픽명(예: "payment.approved"). */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** 직렬화된 이벤트 JSON. 발행 시 이 문자열이 그대로 메시지 값이 된다(직렬화 원천). */
    @Column(nullable = false, length = 2000)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Kafka 발행에 성공해 SENT 로 전이된 시각. 발행 전에는 null. */
    @Column(name = "sent_at")
    private Instant sentAt;

    /** 발행 실패 누적 횟수. {@link #MAX_PUBLISH_RETRIES} 도달 시 DEAD 로 격리한다(M-2). */
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    /** payload 컬럼 한계. 초과분이 DB 까지 내려가 제약 위반 500 이 되기 전에 도메인에서 막는다(M-5). */
    static final int MAX_PAYLOAD_LENGTH = 2000;

    /** 발행 재시도 한계. 초과 시 DEAD 격리(M-2) — poison 이벤트가 뒤 이벤트를 영구히 막는 것을 방지한다. */
    static final int MAX_PUBLISH_RETRIES = 5;

    public OutboxEvent(String aggregateType, Long aggregateId, String eventType, String payload) {
        this.aggregateType = requireText(aggregateType, "애그리거트 종류");
        this.aggregateId = requireNonNull(aggregateId, "애그리거트 id");
        this.eventType = requireText(eventType, "이벤트 종류");
        this.payload = requirePayloadWithinLimit(requireText(payload, "이벤트 페이로드"));
        this.status = OutboxStatus.PENDING;
    }

    private static String requirePayloadWithinLimit(String payload) {
        if (payload.length() > MAX_PAYLOAD_LENGTH) {
            throw new IllegalStateException(
                    "아웃박스 payload 가 최대 길이(" + MAX_PAYLOAD_LENGTH + "자)를 초과했습니다: " + payload.length());
        }
        return payload;
    }

    /** PENDING -> SENT. Kafka 발행 성공 후 호출한다. 이미 SENT 면 거부한다. */
    public void markSent() {
        if (this.status != OutboxStatus.PENDING) {
            throw new IllegalStateException(
                    "PENDING 상태에서만 발행 완료 처리할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = OutboxStatus.SENT;
        this.sentAt = Instant.now();
    }

    /**
     * 발행 실패 1회를 기록한다(M-2). 재시도 한계({@link #MAX_PUBLISH_RETRIES}) 도달 시 DEAD 로 격리하고
     * {@code true} 를 반환한다 — 릴레이는 이를 보고 다음 이벤트로 진행해 poison 이 큐를 막지 않게 한다.
     */
    public boolean recordFailure() {
        if (this.status != OutboxStatus.PENDING) {
            throw new IllegalStateException(
                    "PENDING 상태에서만 발행 실패를 기록할 수 있습니다. 현재 상태: " + this.status);
        }
        this.retryCount++;
        if (this.retryCount >= MAX_PUBLISH_RETRIES) {
            this.status = OutboxStatus.DEAD;
            return true;
        }
        return false;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "은(는) 필수입니다.");
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + "은(는) 필수입니다.");
        }
        return value;
    }
}
