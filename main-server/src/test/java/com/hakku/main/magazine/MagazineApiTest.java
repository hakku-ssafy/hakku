package com.hakku.main.magazine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MagazineApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 주어진 role 로 회원을 만들고 로그인 토큰을 돌려준다. 공개 signup 은 ADMIN 을 거부하므로
     * ADMIN 은 리포지토리로 직접 프로비저닝한다(운영의 시드/프로비저닝 경로를 미러링).
     */
    private String tokenFor(String email, String role) throws Exception {
        if ("ADMIN".equals(role)) {
            if (!userRepository.existsByEmail(email)) {
                userRepository.save(new User(email, "유저",
                        passwordEncoder.encode("secret123"), Role.ADMIN));
            }
        } else {
            mvc.perform(post("/api/auth/signup")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"email":"%s","password":"secret123","nickname":"유저","role":"%s"}
                        """.formatted(email, role)));
        }
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","password":"secret123"}
                            """.formatted(email)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    private static String magazineJson(String title, boolean published) {
        return """
            {"kicker":"EDITORIAL","title":"%s","subtitle":"부제",
             "content":"## 소제목\\n\\n사진과 글 그리고 [데코 스티커](/products/7)",
             "coverImageUrl":"/storage/images/demo","displayOrder":1,"published":%s}
            """.formatted(title, published);
    }

    private long createMagazine(String adminToken, String json) throws Exception {
        String body = mvc.perform(post("/api/admin/magazines")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("ADMIN 매거진 발행 → 201, 공개 단건 조회 → 200 (마크다운 본문 보존)")
    void createAndGet() throws Exception {
        String admin = tokenFor("mag-admin1@hakku.dev", "ADMIN");
        long id = createMagazine(admin, magazineJson("이주의 다꾸", true));

        mvc.perform(get("/api/magazines/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("이주의 다꾸"))
                .andExpect(jsonPath("$.content").value(
                        org.hamcrest.Matchers.containsString("/products/7")));
    }

    @Test
    @DisplayName("공개 목록 조회 → 200 (배열)")
    void listPublic() throws Exception {
        createMagazine(tokenFor("mag-admin2@hakku.dev", "ADMIN"), magazineJson("공개 매거진", true));

        mvc.perform(get("/api/magazines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists());
    }

    @Test
    @DisplayName("미발행(published=false) 매거진은 공개 목록에서 제외, 어드민 목록엔 포함")
    void unpublishedExcludedFromPublic() throws Exception {
        String admin = tokenFor("mag-admin3@hakku.dev", "ADMIN");
        String unique = "미발행-" + System.nanoTime();
        createMagazine(admin, magazineJson(unique, false));

        String publicBody = mvc.perform(get("/api/magazines"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertFalse(publicBody.contains(unique));

        String adminBody = mvc.perform(get("/api/admin/magazines").header("Authorization", admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertTrue(adminBody.contains(unique));
    }

    @Test
    @DisplayName("일반 회원(NORMAL) 매거진 발행 → 403")
    void createByNormal() throws Exception {
        String normal = tokenFor("mag-normal1@hakku.dev", "NORMAL");

        mvc.perform(post("/api/admin/magazines")
                        .header("Authorization", normal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(magazineJson("거부", true)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("토큰 없이 매거진 발행 → 401")
    void createWithoutToken() throws Exception {
        mvc.perform(post("/api/admin/magazines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(magazineJson("거부", true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 매거진 수정 → 200")
    void updateByAdmin() throws Exception {
        String admin = tokenFor("mag-admin4@hakku.dev", "ADMIN");
        long id = createMagazine(admin, magazineJson("수정 전", true));

        mvc.perform(put("/api/admin/magazines/" + id)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(magazineJson("수정 후", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정 후"));
    }

    @Test
    @DisplayName("ADMIN 매거진 삭제 → 204, 이후 조회 → 404")
    void deleteByAdmin() throws Exception {
        String admin = tokenFor("mag-admin5@hakku.dev", "ADMIN");
        long id = createMagazine(admin, magazineJson("삭제 대상", true));

        mvc.perform(delete("/api/admin/magazines/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/magazines/" + id))
                .andExpect(status().isNotFound());
    }
}
