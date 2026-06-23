package com.hakku.payment.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

/**
 * 아웃박스 릴레이가 PENDING 레코드를 토픽=eventType, 키=aggregateId, 값=payload 로 발행하고,
 * 브로커 ack 후에만 {@link OutboxSentMarker} 로 SENT 처리하는지 검증한다(임베디드 Kafka 없이 Mockito).
 * 발행은 트랜잭션 밖에서 수행되므로 마킹은 별도 협력 빈에 위임된다.
 */
@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private OutboxSentMarker outboxSentMarker;

    private OutboxRelay outboxRelay;

    @BeforeEach
    void setUp() {
        outboxRelay = new OutboxRelay(outboxRepository, kafkaTemplate, outboxSentMarker);
    }

    @Test
    @DisplayName("publishes each pending event then marks it SENT after the broker acks")
    void publishesPendingAndMarksSent() {
        var event = new OutboxEvent("PAYMENT", 1L, "payment.approved", "{\"paymentId\":1}");
        when(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> ack = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(ack);

        int published = outboxRelay.publishPending();

        assertEquals(1, published);
        verify(kafkaTemplate).send("payment.approved", "1", "{\"paymentId\":1}");
        verify(outboxSentMarker).markSent(event.getId());
    }

    @Test
    @DisplayName("does not mark an event sent when the broker send fails")
    void leavesPendingOnSendFailure() {
        var event = new OutboxEvent("PAYMENT", 1L, "payment.approved", "{\"paymentId\":1}");
        when(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(event));
        CompletableFuture<SendResult<String, String>> failed =
                CompletableFuture.failedFuture(new RuntimeException("kafka down"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failed);

        int published = outboxRelay.publishPending();

        assertEquals(0, published);
        verify(outboxSentMarker, never()).markSent(any());
    }

    @Test
    @DisplayName("does nothing when there are no pending events")
    void noPendingEventsIsNoop() {
        when(outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of());

        int published = outboxRelay.publishPending();

        assertEquals(0, published);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        verify(outboxSentMarker, never()).markSent(any());
    }
}
