package com.hakku.main.curation;

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
class CurationCardApiTest {

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

    private static String cardJson(String title, String linkUrl, boolean active) {
        String link = linkUrl == null ? "null" : "\"" + linkUrl + "\"";
        return """
            {"kicker":"NEW","title":"%s","subtitle":"부제","body":"<p>본문</p>",
             "imageUrl":"/storage/images/demo","linkUrl":%s,"displayOrder":1,"active":%s}
            """.formatted(title, link, active);
    }

    private long createCard(String adminToken, String json) throws Exception {
        String body = mvc.perform(post("/api/admin/curation-cards")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("ADMIN 카드 생성 → 201, 공개 단건 조회 → 200")
    void createAndGet() throws Exception {
        String admin = tokenFor("admin1@hakku.dev", "ADMIN");
        long id = createCard(admin, cardJson("신상 모음", "/products", true));

        mvc.perform(get("/api/curation-cards/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("신상 모음"))
                .andExpect(jsonPath("$.linkUrl").value("/products"));
    }

    @Test
    @DisplayName("공개 목록 조회 → 200 (배열)")
    void listPublic() throws Exception {
        createCard(tokenFor("admin2@hakku.dev", "ADMIN"), cardJson("공개 카드", "/community", true));

        mvc.perform(get("/api/curation-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists());
    }

    @Test
    @DisplayName("링크 없는 카드는 linkUrl=null 로 저장된다 (매거진 폴백용)")
    void nullLinkPreserved() throws Exception {
        String admin = tokenFor("admin3@hakku.dev", "ADMIN");
        long id = createCard(admin, cardJson("매거진 카드", null, true));

        mvc.perform(get("/api/curation-cards/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkUrl").doesNotExist())
                .andExpect(jsonPath("$.body").value("<p>본문</p>"));
    }

    @Test
    @DisplayName("일반 회원(NORMAL) 카드 생성 → 403")
    void createByNormal() throws Exception {
        String normal = tokenFor("normal-c1@hakku.dev", "NORMAL");

        mvc.perform(post("/api/admin/curation-cards")
                        .header("Authorization", normal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("거부", "/x", true)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("토큰 없이 카드 생성 → 401")
    void createWithoutToken() throws Exception {
        mvc.perform(post("/api/admin/curation-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("거부", "/x", true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 카드 수정 → 200")
    void updateByAdmin() throws Exception {
        String admin = tokenFor("admin4@hakku.dev", "ADMIN");
        long id = createCard(admin, cardJson("수정 전", "/a", true));

        mvc.perform(put("/api/admin/curation-cards/" + id)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cardJson("수정 후", "/b", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정 후"))
                .andExpect(jsonPath("$.linkUrl").value("/b"));
    }

    @Test
    @DisplayName("ADMIN 카드 삭제 → 204, 이후 조회 → 404")
    void deleteByAdmin() throws Exception {
        String admin = tokenFor("admin5@hakku.dev", "ADMIN");
        long id = createCard(admin, cardJson("삭제 대상", "/c", true));

        mvc.perform(delete("/api/admin/curation-cards/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/curation-cards/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("비활성 카드는 공개 목록에서 제외, 어드민 목록엔 포함")
    void inactiveExcludedFromPublic() throws Exception {
        String admin = tokenFor("admin6@hakku.dev", "ADMIN");
        String unique = "비활성-" + System.nanoTime();
        createCard(admin, cardJson(unique, "/d", false));

        String publicBody = mvc.perform(get("/api/curation-cards"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(publicBody.contains(unique));

        String adminBody = mvc.perform(get("/api/admin/curation-cards").header("Authorization", admin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(adminBody.contains(unique));
    }
}
