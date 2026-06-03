package com.hakku.main.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hakku.main.notification.domain.NotificationType;

/** API 응답 및 Redis 저장용 DTO. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
        NotificationType type,
        Long actorId,
        String actorNickname,
        Long postId,
        String postTitlePreview,
        String message,
        long createdAt) {
}
