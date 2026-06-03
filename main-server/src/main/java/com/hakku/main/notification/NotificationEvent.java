package com.hakku.main.notification;

import com.hakku.main.notification.domain.NotificationType;

/**
 * Kafka `notification.created` 토픽에 발행되는 이벤트 페이로드.
 */
public record NotificationEvent(
        NotificationType type,
        Long recipientId,
        Long actorId,
        String message,
        long createdAt) {
}
