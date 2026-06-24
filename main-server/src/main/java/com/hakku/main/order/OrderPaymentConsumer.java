package com.hakku.main.order;

import com.hakku.main.notification.NotificationEvent;
import com.hakku.main.notification.NotificationMessageFormatter;
import com.hakku.main.notification.NotificationProducer;
import com.hakku.main.notification.domain.NotificationType;
import java.time.Instant;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * {@code payment.approved} 토픽을 구독해 주문 결제 완료를 처리한다.
 * referenceType="ORDER" 인 결제만 대상으로 주문을 PAID 로 전이하고, 실제 전이가 일어난 경우에만
 * 구매자에게 ORDER 알림을 발행한다(이벤트 at-least-once 중복 수신 시 알림 중복 방지).
 */
@Component
public class OrderPaymentConsumer {

    private static final String ORDER_REFERENCE = "ORDER";

    private final OrderService orderService;
    private final NotificationProducer notificationProducer;

    public OrderPaymentConsumer(OrderService orderService, NotificationProducer notificationProducer) {
        this.orderService = orderService;
        this.notificationProducer = notificationProducer;
    }

    @KafkaListener(topics = "payment.approved",
                   groupId = "hakku-payment-order-group",
                   containerFactory = "paymentEventKafkaListenerContainerFactory")
    public void consume(PaymentApprovedEvent event) {
        if (event == null || !ORDER_REFERENCE.equals(event.referenceType())) {
            return; // 장바구니 등 주문이 아닌 결제는 무시
        }
        long orderId = Long.parseLong(event.referenceId());
        boolean transitioned = orderService.markPaid(orderId);
        if (transitioned) {
            notificationProducer.publish(new NotificationEvent(
                    NotificationType.ORDER,
                    event.userId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    NotificationMessageFormatter.order(),
                    Instant.now().toEpochMilli()));
        }
    }
}
