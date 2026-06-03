package com.hakku.main.notification;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hakku.main.notification.domain.NotificationType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationApiTest {

    @Autowired MockMvc mvc;
    @MockitoBean NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenFor(String email) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"secret123","nickname":"알림유저","role":"NORMAL"}
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
    @DisplayName("GET /api/notifications → 200 + 알림 목록")
    void getNotifications_returns200() throws Exception {
        String token = tokenFor("notif1@hakku.dev");
        given(notificationService.getNotifications(anyLong())).willReturn(List.of(
                new NotificationResponse(NotificationType.COMMENT, 99L, "테스터", 1L, "제목", "댓글을 달았습니다.",
                        Instant.now().toEpochMilli())
        ));

        mvc.perform(get("/api/notifications").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("COMMENT"))
                .andExpect(jsonPath("$[0].message").value("댓글을 달았습니다."));
    }

    @Test
    @DisplayName("GET /api/notifications 인증 없음 → 401")
    void getNotifications_unauthorized() throws Exception {
        mvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/notifications 알림 없음 → 200 + 빈 배열")
    void getNotifications_returnsEmptyArray() throws Exception {
        String token = tokenFor("notif2@hakku.dev");
        given(notificationService.getNotifications(anyLong())).willReturn(List.of());

        mvc.perform(get("/api/notifications").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
