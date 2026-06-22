package com.hakku.main.common.config;

import javax.sql.DataSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * MyBatis 수동 설정.
 *
 * <p>Spring Boot 4.0(Spring Framework 7)에서는 {@code mybatis-spring-boot-starter}의 자동구성이
 * 동작하지 않는다(Phase 0 호환성 검증 결과). 따라서 mybatis-spring 코어를 직접 구성한다:
 * <ul>
 *   <li>{@link SqlSessionFactory} — DataSource 연결, XML 매퍼 위치, 언더스코어→카멜 변환</li>
 *   <li>{@link SqlSessionTemplate} — 스레드 안전한 SqlSession, Spring 트랜잭션 동기화</li>
 *   <li>{@link MapperScan} — {@code @Mapper} 인터페이스 자동 빈 등록</li>
 * </ul>
 * enum은 MyBatis 기본 {@code EnumTypeHandler}(name 기반)로 매핑되어 기존
 * {@code @Enumerated(STRING)}와 동일하게 동작한다.
 */
@Configuration
@MapperScan(basePackages = "com.hakku.main", annotationClass = Mapper.class)
public class MyBatisConfig {

    private static final String MAPPER_LOCATION_PATTERN = "classpath*:mapper/**/*.xml";

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        Resource[] mapperLocations = new PathMatchingResourcePatternResolver()
                .getResources(MAPPER_LOCATION_PATTERN);
        factory.setMapperLocations(mapperLocations);

        org.apache.ibatis.session.Configuration configuration =
                new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);

        return factory.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    /**
     * 트랜잭션 매니저. JPA(starter-data-jpa) 제거로 {@code JpaTransactionManager}가 사라지므로
     * {@code @Transactional}이 계속 동작하도록 명시적으로 등록한다.
     * {@link org.springframework.jdbc.support.JdbcTransactionManager}는
     * {@code DataSourceTransactionManager}의 현대적 서브클래스다.
     */
    @Bean
    public org.springframework.jdbc.support.JdbcTransactionManager transactionManager(javax.sql.DataSource ds) {
        return new org.springframework.jdbc.support.JdbcTransactionManager(ds);
    }
}
