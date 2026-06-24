package com.hakku.payment.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 아웃박스 릴레이의 스케줄 트리거. 워커({@link OutboxRelay})와 분리해, 폴링은 운영 환경에서만 켜지도록 한다.
 *
 * <p>{@code payment.outbox.relay.enabled=true} 일 때만 이 구성이 등록되며, 그때만 {@code @EnableScheduling}
 * 이 활성화된다. 따라서 테스트(@SpringBootTest)에서는 이 빈이 만들어지지 않아 죽은 브로커를 폴링하는 일이 없다.
 * 테스트는 {@link OutboxRelay#publishPending()} 을 직접 호출해 결정적으로 검증한다.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "payment.outbox.relay.enabled", havingValue = "true")
public class OutboxRelayScheduler {

    private final OutboxRelay outboxRelay;

    public OutboxRelayScheduler(OutboxRelay outboxRelay) {
        this.outboxRelay = outboxRelay;
    }

    @Scheduled(fixedDelayString = "${payment.outbox.relay.interval-ms:2000}")
    public void poll() {
        outboxRelay.publishPending();
    }
}
