package com.hakku.payment.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 트랜잭셔널 아웃박스 레코드의 상태 머신과 불변식을 검증한다.
 * 결제 상태 변경과 같은 트랜잭션에서 기록되며, 릴레이가 발행 후 SENT 로 전이시킨다.
 */
class OutboxEventTest {

    private OutboxEvent pendingEvent() {
        return new OutboxEvent("PAYMENT", 1L, "payment.approved", "{\"paymentId\":1}");
    }

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("a new event starts in PENDING with no sentAt")
        void newEventStartsPending() {
            var event = pendingEvent();

            assertEquals(OutboxStatus.PENDING, event.getStatus());
            assertNull(event.getSentAt());
        }

        @Test
        @DisplayName("a new event exposes its aggregate, type and payload")
        void newEventExposesFields() {
            var event = pendingEvent();

            assertEquals("PAYMENT", event.getAggregateType());
            assertEquals(1L, event.getAggregateId());
            assertEquals("payment.approved", event.getEventType());
            assertEquals("{\"paymentId\":1}", event.getPayload());
        }

        @Test
        @DisplayName("aggregate type is required")
        void aggregateTypeRequired() {
            assertThrows(IllegalArgumentException.class,
                    () -> new OutboxEvent(" ", 1L, "payment.approved", "{}"));
        }

        @Test
        @DisplayName("aggregate id is required")
        void aggregateIdRequired() {
            assertThrows(IllegalArgumentException.class,
                    () -> new OutboxEvent("PAYMENT", null, "payment.approved", "{}"));
        }

        @Test
        @DisplayName("event type is required")
        void eventTypeRequired() {
            assertThrows(IllegalArgumentException.class,
                    () -> new OutboxEvent("PAYMENT", 1L, " ", "{}"));
        }

        @Test
        @DisplayName("payload is required")
        void payloadRequired() {
            assertThrows(IllegalArgumentException.class,
                    () -> new OutboxEvent("PAYMENT", 1L, "payment.approved", " "));
        }
    }

    @Nested
    @DisplayName("markSent")
    class MarkSent {

        @Test
        @DisplayName("marking a pending event sent transitions to SENT and records sentAt")
        void marksSent() {
            var event = pendingEvent();

            event.markSent();

            assertEquals(OutboxStatus.SENT, event.getStatus());
            assertNotNull(event.getSentAt());
        }

        @Test
        @DisplayName("marking an already-sent event sent again is rejected")
        void marksSentTwiceRejected() {
            var event = pendingEvent();
            event.markSent();

            assertThrows(IllegalStateException.class, event::markSent);
        }
    }
}
