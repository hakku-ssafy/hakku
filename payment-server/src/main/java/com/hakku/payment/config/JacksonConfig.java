package com.hakku.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 애플리케이션 전역 {@link ObjectMapper} 빈.
 *
 * <p>이 설정에서는 주입 가능한 독립 {@code ObjectMapper} 빈이 컨텍스트에 없어 {@code OutboxRecorder} 주입이
 * 실패하는 것을 확인했다. 아웃박스 직렬화와 이후 웹훅/소비자가 공용으로 주입해 쓰도록 한곳에서 명시 제공한다.
 * (Task 5 에서 컨트롤러 JSON 직렬화가 이 빈을 사용하는지 확인 필요.)
 *
 * <p>클래스패스의 Jackson 모듈(jsr310 등)을 자동 등록해 {@code Instant} 등 시간 타입 직렬화도 일관되게 동작한다.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().findAndAddModules().build();
    }
}
