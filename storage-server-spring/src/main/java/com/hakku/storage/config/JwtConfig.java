package com.hakku.storage.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hakku.storage.auth.JwtProperties;
import com.hakku.storage.auth.JwtValidator;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

	@Bean
	@ConditionalOnExpression("!'${jwt.secret:}'.isEmpty()")
	JwtValidator jwtValidator(JwtProperties properties) {
		return new JwtValidator(properties.secret());
	}
}
