package com.hakku.payment.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * M-1: 액추에이터 노출 축소(방어 심화). nginx 가 /actuator 를 외부로 프록시하지 않지만, 내부 직접 접근 대비
 * (1) 헬스 세부는 비인증에 노출하지 않고(when-authorized), (2) 브라우징 가능한 /actuator/metrics 는 미노출한다.
 * /actuator/prometheus 는 내부 스크레이핑을 위해 유지한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorExposureTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("/actuator/metrics 는 노출하지 않는다 → 404")
    void metricsEndpointNotExposed() throws Exception {
        mvc.perform(get("/actuator/metrics")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("/actuator/health 는 비인증 호출자에게 컴포넌트 세부를 노출하지 않는다")
    void healthHidesDetailsFromAnonymous() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.components").doesNotExist());
    }
}
