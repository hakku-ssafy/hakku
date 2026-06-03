package com.hakku.main.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class UserApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** 회원가입 + 로그인 후 Bearer 토큰을 반환한다. */
    private String tokenFor(String email) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"secret123","nickname":"지호","role":"NORMAL"}
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
    @DisplayName("GET /api/users/me → 200 + 내 프로필")
    void getMe() throws Exception {
        String token = tokenFor("me1@hakku.dev");

        mvc.perform(get("/api/users/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me1@hakku.dev"))
                .andExpect(jsonPath("$.nickname").value("지호"))
                .andExpect(jsonPath("$.role").value("NORMAL"));
    }

    @Test
    @DisplayName("PUT /api/users/me → 200 + 갱신된 프로필")
    void updateMe() throws Exception {
        String token = tokenFor("me2@hakku.dev");

        mvc.perform(put("/api/users/me")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nickname":"지호2","profileImageUrl":"https://img/p.png",
                             "preferredStyles":["캐주얼","미니멀"]}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("지호2"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://img/p.png"));
    }
}
