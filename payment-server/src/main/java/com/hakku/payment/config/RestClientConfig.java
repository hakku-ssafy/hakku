package com.hakku.payment.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 이 모듈(Spring Boot 4 webmvc 모듈러 구성)에서는 {@code RestClient.Builder} 자동설정 빈이 노출되지 않아,
 * 토스 결제 승인 클라이언트({@code RestClientTossPaymentClient})가 주입받을 수 없다(컨텍스트 로딩 실패).
 * 빌더 빈을 명시적으로 등록한다.
 *
 * <p>H-1: connect/read 타임아웃을 명시한다. 타임아웃이 없으면 토스 confirm 무응답 시 read 가 무한 대기하여
 * 톰캣 워커 스레드를 무기한 점유 → 결제 경로 전체가 마비된다. Kafka SEND_TIMEOUT(10s)·request.timeout(3s)와
 * 같은 규율로 connect 3s / read 8s 를 기본값으로 둔다(프로퍼티로 조정 가능).
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder(
            @Value("${payment.toss.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${payment.toss.read-timeout-ms:8000}") long readTimeoutMs) {
        return RestClient.builder().requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs));
    }

    /** connect/read 타임아웃을 건 요청 팩토리. 토스 무응답 시 워커 스레드 무한 점유를 막는다. */
    public static ClientHttpRequestFactory requestFactory(long connectTimeoutMs, long readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return factory;
    }
}
