package com.hakku.main.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Redis List 기반 알림 저장소.
 * Key: {@code user:{userId}:notifications}
 * LPUSH → 최신 먼저 정렬, LTRIM으로 최근 MAX_NOTIFICATIONS개 유지.
 */
@Repository
public class NotificationRedisRepository {

    public static final int MAX_NOTIFICATIONS = 50;
    private static final String KEY_PREFIX = "user:%d:notifications";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Autowired
    public NotificationRedisRepository(StringRedisTemplate redis) {
        this.redis = redis;
        this.objectMapper = new ObjectMapper();
    }

    /** 테스트에서 ObjectMapper를 주입할 수 있도록 제공하는 생성자. */
    NotificationRedisRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void save(Long userId, NotificationResponse dto) {
        String key = key(userId);
        try {
            String json = objectMapper.writeValueAsString(dto);
            redis.opsForList().leftPush(key, json);
            redis.opsForList().trim(key, 0, MAX_NOTIFICATIONS - 1);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("알림 직렬화 실패", e);
        }
    }

    public List<NotificationResponse> findAll(Long userId) {
        List<String> jsons = redis.opsForList().range(key(userId), 0, MAX_NOTIFICATIONS - 1);
        if (jsons == null) return List.of();
        return jsons.stream()
                .map(this::deserialize)
                .filter(Objects::nonNull)
                .toList();
    }

    private String key(Long userId) {
        return String.format(KEY_PREFIX, userId);
    }

    private NotificationResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, NotificationResponse.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
