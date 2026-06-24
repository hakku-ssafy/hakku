package com.hakku.payment.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hakku.payment.payment.PaymentTopics;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

/**
 * 트랜잭셔널 아웃박스 종단 검증(임베디드 Kafka): PENDING 레코드가 실제 토픽으로 발행되고 SENT 로 전이되는지 확인한다.
 *
 * <p>스케줄러를 켜지 않고 {@link OutboxRelay#publishPending()} 을 직접 호출해 결정적으로 검증한다.
 * 브로커 주소는 {@code spring.embedded.kafka.brokers} 로 주입된 임베디드 브로커를 가리킨다.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka(partitions = 1, topics = PaymentTopics.APPROVED)
class OutboxRelayIntegrationTest {

    @Autowired
    private OutboxRepository outboxRepository;
    @Autowired
    private OutboxRelay outboxRelay;
    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private Consumer<String, String> consumer;

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("a pending outbox event is published to its topic and marked SENT")
    void publishesPendingEventEndToEnd() {
        String payload = "{\"paymentId\":1,\"amount\":15000}";
        var saved = outboxRepository.save(
                new OutboxEvent("PAYMENT", 1L, PaymentTopics.APPROVED, payload));

        Map<String, Object> consumerProps =
                KafkaTestUtils.consumerProps("outbox-it-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps, new StringDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, PaymentTopics.APPROVED);

        int published = outboxRelay.publishPending();

        assertEquals(1, published);
        ConsumerRecord<String, String> record =
                KafkaTestUtils.getSingleRecord(consumer, PaymentTopics.APPROVED, Duration.ofSeconds(10));
        assertEquals("1", record.key());
        assertEquals(payload, record.value());
        assertEquals(OutboxStatus.SENT,
                outboxRepository.findById(saved.getId()).orElseThrow().getStatus());
    }
}
