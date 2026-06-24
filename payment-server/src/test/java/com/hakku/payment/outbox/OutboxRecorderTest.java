package com.hakku.payment.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 도메인 이벤트를 JSON 으로 직렬화해 PENDING 아웃박스 레코드로 저장하는지 검증한다(DB 없이 Mockito).
 */
@ExtendWith(MockitoExtension.class)
class OutboxRecorderTest {

    @Mock
    private OutboxRepository outboxRepository;

    private OutboxRecorder outboxRecorder;

    @BeforeEach
    void setUp() {
        outboxRecorder = new OutboxRecorder(outboxRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("records the serialized payload as a PENDING outbox event")
    void recordsSerializedPendingEvent() {
        when(outboxRepository.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        outboxRecorder.record("PAYMENT", 1L, "payment.approved", Map.of("paymentId", 1L));

        var captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        var saved = captor.getValue();

        assertEquals("PAYMENT", saved.getAggregateType());
        assertEquals(1L, saved.getAggregateId());
        assertEquals("payment.approved", saved.getEventType());
        assertEquals("{\"paymentId\":1}", saved.getPayload());
        assertEquals(OutboxStatus.PENDING, saved.getStatus());
    }
}
