package com.hakku.main.notification;

import static org.mockito.Mockito.verify;

import com.hakku.main.notification.domain.NotificationType;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock NotificationService notificationService;
    @InjectMocks NotificationConsumer consumer;

    @Test
    @DisplayName("Kafka 이벤트 수신 시 NotificationService.saveNotification() 호출")
    void consume_savesNotificationViaService() {
        NotificationEvent event = new NotificationEvent(
                NotificationType.COMMENT, 1L, 2L, "테스터", 10L, "제목", "댓글을 달았습니다.", Instant.now().toEpochMilli());

        consumer.consume(event);

        verify(notificationService).saveNotification(event);
    }
}
