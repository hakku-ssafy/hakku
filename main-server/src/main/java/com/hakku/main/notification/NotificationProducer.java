package com.hakku.main.notification;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** `notification.created` 토픽에 이벤트를 fire-and-forget 방식으로 발행한다. */
@Component
public class NotificationProducer {

    static final String TOPIC = "notification.created";

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public NotificationProducer(KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(NotificationEvent event) {
        kafkaTemplate.send(TOPIC, event);
    }
}
