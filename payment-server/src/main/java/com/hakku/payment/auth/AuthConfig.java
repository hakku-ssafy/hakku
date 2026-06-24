package com.hakku.payment.auth;

import com.hakku.payment.auth.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 인증 관련 빈 구성. 결제 서버는 토큰을 발급하지 않고 검증만 하므로 검증 전용 {@link JwtTokenProvider} 만 제공한다.
 * 비밀키는 main-server 와 공유한다({@code jwt.secret}).
 */
@Configuration
public class AuthConfig {

    @Bean
    public JwtTokenProvider jwtTokenProvider(@Value("${jwt.secret}") String secret) {
        return new JwtTokenProvider(secret);
    }
}
