package com.hakku.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 이 모듈(Spring Boot 4 webmvc 모듈러 구성)에서는 {@code RestClient.Builder} 자동설정 빈이 노출되지 않아,
 * 토스 결제 승인 클라이언트({@code RestClientTossPaymentClient})가 주입받을 수 없다(컨텍스트 로딩 실패).
 * 빌더 빈을 명시적으로 등록한다. 빌더 단위라 호출 측에서 baseUrl/헤더를 자유롭게 구성한다.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
