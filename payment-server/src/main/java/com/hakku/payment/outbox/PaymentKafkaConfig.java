package com.hakku.payment.outbox;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * 아웃박스 릴레이 전용 Kafka producer 설정.
 *
 * <p>알림 발행과 달리 릴레이는 요청 경로 밖의 백그라운드 작업이므로 <b>지연이 아니라 신뢰성</b>을 우선한다.
 * acks=all + 멱등 producer + 초 단위 delivery timeout 으로 일시적 브로커 장애에도 끈질기게 전송한다.
 * 페이로드는 이미 직렬화된 JSON 문자열이므로 값 직렬화는 {@link StringSerializer} 를 쓴다(이중 직렬화 방지).
 *
 * <p>직접 {@code KafkaTemplate} 빈을 정의하므로 Spring Boot 의 자동 KafkaTemplate
 * ({@code @ConditionalOnMissingBean})은 비활성화된다.
 */
@Configuration
public class PaymentKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> outboxProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // delivery timeout(8s) >= request timeout(3s); 릴레이의 .get(10s)보다 짧게 두어 producer 가
        // .get 만료 전에 성공/실패를 확정하도록 한다(좀비 전송 → 다음 폴링 중복 발행 방지).
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 8_000);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3_000);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate(
            ProducerFactory<String, String> outboxProducerFactory) {
        return new KafkaTemplate<>(outboxProducerFactory);
    }
}
