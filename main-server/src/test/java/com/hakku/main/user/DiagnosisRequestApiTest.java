package com.hakku.main.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hakku.main.notification.NotificationProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DiagnosisRequestApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    @SuppressWarnings("unused")
    private NotificationProducer notificationProducer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenFor(String email) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"secret123","nickname":"진단테스터","role":"NORMAL"}
                    """.formatted(email)));
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","password":"secret123"}
                            """.formatted(email)))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return "Bearer " + node.get("accessToken").asText();
    }

    @Test
    @DisplayName("POST /api/users/me/diagnosis-request → 200 (최초 요청)")
    void requestDiagnosis_firstTime_returns200() throws Exception {
        String token = tokenFor("dreq1@hakku.dev");

        mvc.perform(post("/api/users/me/diagnosis-request")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/users/me/diagnosis-request 두 번 → 409")
    void requestDiagnosis_duplicate_returns409() throws Exception {
        String token = tokenFor("dreq2@hakku.dev");

        mvc.perform(post("/api/users/me/diagnosis-request")
                .header("Authorization", token));

        mvc.perform(post("/api/users/me/diagnosis-request")
                        .header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/users/me/diagnosis-request 인증 없음 → 401")
    void requestDiagnosis_noAuth_returns401() throws Exception {
        mvc.perform(post("/api/users/me/diagnosis-request"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/users/me/diagnosis-request → 204 (상태 초기화)")
    void resetDiagnosis_returns204() throws Exception {
        String token = tokenFor("dreq3@hakku.dev");

        mvc.perform(post("/api/users/me/diagnosis-request")
                .header("Authorization", token));

        mvc.perform(delete("/api/users/me/diagnosis-request")
                        .header("Authorization", token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("초기화 후 재요청 → 200 (NONE으로 복구됨)")
    void requestDiagnosis_afterReset_returns200() throws Exception {
        String token = tokenFor("dreq4@hakku.dev");

        mvc.perform(post("/api/users/me/diagnosis-request")
                .header("Authorization", token));
        mvc.perform(delete("/api/users/me/diagnosis-request")
                .header("Authorization", token));

        mvc.perform(post("/api/users/me/diagnosis-request")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }
}
