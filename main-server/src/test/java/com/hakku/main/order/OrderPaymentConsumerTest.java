package com.hakku.main.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.main.notification.NotificationEvent;
import com.hakku.main.notification.NotificationProducer;
import com.hakku.main.notification.domain.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderPaymentConsumerTest {

    @Mock
    private OrderService orderService;
    @Mock
    private NotificationProducer notificationProducer;

    private OrderPaymentConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderPaymentConsumer(orderService, notificationProducer);
    }

    private PaymentApprovedEvent event(String referenceType, String referenceId, Long userId) {
        return new PaymentApprovedEvent(1L, userId, referenceType, referenceId,
                9800L, "KRW", "APPROVED", "tx_1", 0L);
    }

    @Test
    @DisplayName("ORDER 결제 승인 → 주문 PAID 전이 + ORDER 알림 발행(수신자=구매자)")
    void orderApproved_marksPaidAndNotifies() {
        when(orderService.markPaid(42L)).thenReturn(true);

        consumer.consume(event("ORDER", "42", 7L));

        verify(orderService).markPaid(42L);
        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationProducer).publish(captor.capture());
        NotificationEvent published = captor.getValue();
        assertThat(published.type()).isEqualTo(NotificationType.ORDER);
        assertThat(published.recipientId()).isEqualTo(7L);
        assertThat(published.message()).isNotBlank();
    }

    @Test
    @DisplayName("ORDER 가 아닌 결제(예: CART)는 무시한다")
    void nonOrderReference_ignored() {
        consumer.consume(event("CART", "cart", 7L));

        verify(orderService, never()).markPaid(any());
        verify(notificationProducer, never()).publish(any());
    }

    @Test
    @DisplayName("이미 처리된 주문(markPaid=false)이면 알림을 발행하지 않는다(멱등)")
    void alreadyPaid_noDuplicateNotification() {
        when(orderService.markPaid(42L)).thenReturn(false);

        consumer.consume(event("ORDER", "42", 7L));

        verify(notificationProducer, never()).publish(any());
    }
}
