package com.hakku.main.notification;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** `notification.created` 토픽을 구독해 Redis에 저장한다. */
@Component
public class NotificationConsumer {

    private final NotificationService notificationService;

    public NotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = NotificationProducer.TOPIC,
                   groupId = "hakku-notification-group",
                   containerFactory = "notificationKafkaListenerContainerFactory")
    public void consume(NotificationEvent event) {
        notificationService.saveNotification(event);
    }
}
