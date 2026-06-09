package com.hakku.main.notification;

import java.util.List;
import org.springframework.stereotype.Service;

/** 알림 저장(Kafka 소비 경로)과 조회(API 경로)를 담당한다. */
@Service
public class NotificationService {

    private final NotificationRedisRepository repository;

    public NotificationService(NotificationRedisRepository repository) {
        this.repository = repository;
    }

    /** Kafka consumer가 이벤트를 받으면 Redis에 저장한다. */
    public void saveNotification(NotificationEvent event) {
        NotificationResponse dto = new NotificationResponse(
                event.type(),
                event.actorId(),
                event.actorNickname(),
                event.postId(),
                event.postTitlePreview(),
                event.productId(),
                event.message(),
                event.createdAt());
        repository.save(event.recipientId(), dto);
    }

    /** 로그인 사용자의 최근 알림 목록을 Redis에서 조회한다. */
    public List<NotificationResponse> getNotifications(Long userId) {
        return repository.findAll(userId);
    }
}
