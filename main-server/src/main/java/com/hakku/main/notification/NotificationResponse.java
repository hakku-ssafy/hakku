package com.hakku.main.notification;

import com.hakku.main.notification.domain.NotificationType;

/** API 응답 및 Redis 저장용 DTO. */
public record NotificationResponse(
        NotificationType type,
        Long actorId,
        String message,
        long createdAt) {
}
