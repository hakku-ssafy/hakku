package com.hakku.main.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hakku.main.notification.domain.NotificationType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ListOperations<String, String> listOps;

    private NotificationRedisRepository repository;
    private NotificationService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForList()).willReturn(listOps);
        repository = new NotificationRedisRepository(redisTemplate, objectMapper);
        service = new NotificationService(repository);
    }

    @Test
    @DisplayName("getNotifications - Redis에서 알림 목록 반환")
    void getNotifications_returnsListFromRedis() throws Exception {
        long now = Instant.now().toEpochMilli();
        String json = objectMapper.writeValueAsString(
                new NotificationResponse(NotificationType.COMMENT, 2L, "테스트 댓글", now));
        given(listOps.range(anyString(), anyLong(), anyLong())).willReturn(List.of(json));

        List<NotificationResponse> result = service.getNotifications(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(NotificationType.COMMENT);
        assertThat(result.get(0).message()).isEqualTo("테스트 댓글");
    }

    @Test
    @DisplayName("getNotifications - 알림 없으면 빈 리스트")
    void getNotifications_returnsEmptyWhenNone() {
        given(listOps.range(anyString(), anyLong(), anyLong())).willReturn(List.of());

        List<NotificationResponse> result = service.getNotifications(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("saveNotification - Redis에 LPUSH 후 LTRIM")
    void saveNotification_pushesToRedisAndTrims() {
        NotificationEvent event = new NotificationEvent(
                NotificationType.COMMENT, 1L, 2L, "댓글을 달았습니다.", Instant.now().toEpochMilli());

        service.saveNotification(event);

        verify(listOps).leftPush(eq("user:1:notifications"), anyString());
        verify(listOps).trim(eq("user:1:notifications"), eq(0L),
                eq((long) NotificationRedisRepository.MAX_NOTIFICATIONS - 1));
    }
}
