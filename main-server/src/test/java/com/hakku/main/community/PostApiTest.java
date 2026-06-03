package com.hakku.main.community;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class PostApiTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenFor(String email) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"secret123","nickname":"작성자","role":"NORMAL"}
                    """.formatted(email)));
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","password":"secret123"}
                            """.formatted(email)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    private long createPost(String token, String title) throws Exception {
        String body = mvc.perform(post("/api/posts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title":"%s","content":"본문 내용"}
                            """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("POST /api/posts → 201, GET /api/posts/{id} → 200 (공개 조회)")
    void createAndGet() throws Exception {
        String token = tokenFor("author1@hakku.dev");
        long id = createPost(token, "첫 글");

        mvc.perform(get("/api/posts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("첫 글"));
    }

    @Test
    @DisplayName("GET /api/posts → 200 목록 (공개)")
    void listPublic() throws Exception {
        String token = tokenFor("author2@hakku.dev");
        createPost(token, "목록용 글");

        mvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").exists());
    }

    @Test
    @DisplayName("POST /api/posts 토큰 없음 → 401")
    void createWithoutToken() throws Exception {
        mvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title":"x","content":"y"}
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/posts/{id} 작성자 본인 → 200")
    void updateByAuthor() throws Exception {
        String token = tokenFor("author3@hakku.dev");
        long id = createPost(token, "수정 전");

        mvc.perform(put("/api/posts/" + id)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title":"수정 후","content":"바뀐 본문"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정 후"));
    }

    @Test
    @DisplayName("PUT /api/posts/{id} 타인 → 403")
    void updateByOther() throws Exception {
        long id = createPost(tokenFor("owner1@hakku.dev"), "남의 글");
        String otherToken = tokenFor("intruder1@hakku.dev");

        mvc.perform(put("/api/posts/" + id)
                        .header("Authorization", otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title":"가로채기","content":"악의적 수정"}
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} 작성자 본인 → 204")
    void deleteByAuthor() throws Exception {
        String token = tokenFor("author4@hakku.dev");
        long id = createPost(token, "삭제될 글");

        mvc.perform(delete("/api/posts/" + id).header("Authorization", token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/posts/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} 타인 → 403")
    void deleteByOther() throws Exception {
        long id = createPost(tokenFor("owner2@hakku.dev"), "지켜질 글");
        String otherToken = tokenFor("intruder2@hakku.dev");

        mvc.perform(delete("/api/posts/" + id).header("Authorization", otherToken))
                .andExpect(status().isForbidden());
    }
}
