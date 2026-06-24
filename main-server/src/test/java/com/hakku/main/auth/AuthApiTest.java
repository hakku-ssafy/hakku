package com.hakku.main.auth;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
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

    private static final String REFRESH_COOKIE = "refreshToken";

    @Autowired
    private MockMvc mvc;

    private String signupJson(String email) {
        return """
            {"email":"%s","password":"secret123","nickname":"테스터","role":"NORMAL"}
            """.formatted(email);
    }

    private String loginJson(String email) {
        return """
            {"email":"%s","password":"secret123"}
            """.formatted(email);
    }

    /** 회원가입 + 로그인 후 발급된 리프레시 쿠키를 돌려준다. */
    private Cookie signupLoginRefreshCookie(String email) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(signupJson(email)));
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginJson(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(REFRESH_COOKIE);
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
    @DisplayName("POST /api/auth/signup: role=ADMIN 이면 403 (자가 권한 상승 차단)")
    void signupRejectsAdminRole() throws Exception {
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"evil@hakku.dev","password":"secret123","nickname":"해커","role":"ADMIN"}
                            """))
                .andExpect(status().isForbidden());
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

    @Test
    @DisplayName("로그인 → httpOnly 리프레시 토큰 쿠키를 Set-Cookie 로 내려준다")
    void loginSetsHttpOnlyRefreshCookie() throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON).content(signupJson("rc1@hakku.dev")));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginJson("rc1@hakku.dev")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(cookie().exists(REFRESH_COOKIE))
                .andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
                .andExpect(cookie().value(REFRESH_COOKIE, not(emptyString())));
    }

    @Test
    @DisplayName("유효한 리프레시 쿠키로 /api/auth/refresh → 200 + 새 accessToken")
    void refreshWithValidCookieReturnsNewAccessToken() throws Exception {
        Cookie refresh = signupLoginRefreshCookie("rc2@hakku.dev");

        mvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())));
    }

    @Test
    @DisplayName("쿠키 없이 /api/auth/refresh → 401")
    void refreshWithoutCookieIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("위조/형식오류 리프레시 쿠키로 /api/auth/refresh → 401")
    void refreshWithInvalidCookieIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, "not.a.jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃 → 리프레시 쿠키를 만료(Max-Age=0) 시킨다")
    void logoutClearsRefreshCookie() throws Exception {
        mvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 0));
    }

    @Test
    @DisplayName("리프레시 토큰을 Bearer 액세스 토큰으로 보호 경로에 쓰면 401 (필터가 type=refresh 거부)")
    void refreshTokenCannotBeUsedAsAccessToken() throws Exception {
        Cookie refresh = signupLoginRefreshCookie("rc3@hakku.dev");

        mvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + refresh.getValue()))
                .andExpect(status().isUnauthorized());
    }
}
