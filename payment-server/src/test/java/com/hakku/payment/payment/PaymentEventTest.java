package com.hakku.payment.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.hakku.payment.payment.domain.Payment;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * M-3: 아웃박스 이벤트의 occurredAt 은 "릴레이가 발행한 시각"이 아니라 "결제 상태가 전이된 시각"이어야 한다.
 * 릴레이 지연 시 발행시각(now)을 쓰면 occurredAt 이 실제 전이보다 늦어 부정확하다.
 */
class PaymentEventTest {

    @Test
    @DisplayName("occurredAt 은 발행 시각(now)이 아니라 결제 전이 시각(updatedAt)이다")
    void occurredAtIsTransitionTimeNotNow() {
        Payment payment = new Payment(7L, "CART", "cart-42", 15000L, "KRW", "MOCK", "idem-m3");
        Instant transitionedAt = Instant.parse("2026-06-24T00:00:00Z");
        ReflectionTestUtils.setField(payment, "id", 1L);
        ReflectionTestUtils.setField(payment, "updatedAt", transitionedAt);

        PaymentEvent event = PaymentEvent.from(payment);

        assertThat(event.occurredAt()).isEqualTo(transitionedAt.toEpochMilli());
    }
}
