package com.hakku.main.auth;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired
    private MockMvc mvc;

    private String signupJson(String email) {
        return """
            {"email":"%s","password":"secret123","nickname":"테스터","role":"NORMAL"}
            """.formatted(email);
    }

    @Test
    @DisplayName("POST /api/auth/signup → 201 + userId")
    void signup() throws Exception {
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("s1@hakku.dev")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber());
    }

    @Test
    @DisplayName("POST /api/auth/login → 200 + accessToken")
    void login() throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson("s2@hakku.dev")));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"s2@hakku.dev","password":"secret123"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())));
    }

    @Test
    @DisplayName("중복 이메일 회원가입 → 409")
    void duplicateSignup() throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson("dup-api@hakku.dev")));

        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupJson("dup-api@hakku.dev")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("잘못된 비밀번호 로그인 → 401")
    void wrongPasswordLogin() throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupJson("s3@hakku.dev")));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"s3@hakku.dev","password":"wrong"}
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 없이 보호된 경로 접근 → 401")
    void protectedWithoutToken() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("입력 검증 실패(빈 이메일) → 400")
    void invalidSignup() throws Exception {
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"","password":"secret123","nickname":"x","role":"NORMAL"}
                            """))
                .andExpect(status().isBadRequest());
    }
}
