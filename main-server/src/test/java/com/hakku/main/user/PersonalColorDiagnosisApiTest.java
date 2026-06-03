package com.hakku.main.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PersonalColorDiagnosisApiTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenFor(String email) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"secret123","nickname":"진단유저","role":"NORMAL"}
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
    @DisplayName("PATCH /api/users/me/personal-color → 200 + 퍼스널컬러·이미지 URL 갱신")
    void updatePersonalColor() throws Exception {
        String token = tokenFor("diag1@hakku.dev");

        mvc.perform(patch("/api/users/me/personal-color")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"personalColor":"LIGHT_SUMMER",
                             "resultImageUrl":"https://storage/result/abc.png"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalColor").value("LIGHT_SUMMER"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://storage/result/abc.png"));
    }

    @Test
    @DisplayName("PATCH /api/users/me/personal-color 인증 없음 → 401")
    void updatePersonalColorUnauthorized() throws Exception {
        mvc.perform(patch("/api/users/me/personal-color")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"personalColor":"LIGHT_SUMMER",
                             "resultImageUrl":"https://storage/result/abc.png"}
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /api/users/me/personal-color 잘못된 타입 → 400")
    void updatePersonalColorInvalidType() throws Exception {
        String token = tokenFor("diag2@hakku.dev");

        mvc.perform(patch("/api/users/me/personal-color")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"personalColor":"NOT_EXIST_TYPE",
                             "resultImageUrl":"https://storage/result/abc.png"}
                            """))
                .andExpect(status().isBadRequest());
    }
}
