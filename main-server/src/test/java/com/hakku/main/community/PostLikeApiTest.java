package com.hakku.main.community;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class PostLikeApiTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenFor(String email) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"secret123","nickname":"좋아요러","role":"NORMAL"}
                    """.formatted(email)));
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","password":"secret123"}
                            """.formatted(email)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    private long createPost(String token) throws Exception {
        String body = mvc.perform(post("/api/posts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title":"좋아요 대상","content":"본문"}
                            """))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("POST 좋아요 → liked=true, count=1; 다시 → liked=false, count=0")
    void toggleLike() throws Exception {
        String token = tokenFor("l1@hakku.dev");
        long postId = createPost(token);

        mvc.perform(post("/api/posts/" + postId + "/likes").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likeCount").value(1));

        mvc.perform(post("/api/posts/" + postId + "/likes").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.likeCount").value(0));
    }

    @Test
    @DisplayName("POST 좋아요 토큰 없음 → 401")
    void likeWithoutToken() throws Exception {
        long postId = createPost(tokenFor("l2@hakku.dev"));

        mvc.perform(post("/api/posts/" + postId + "/likes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST 좋아요: 없는 게시글 → 404")
    void likeMissingPost() throws Exception {
        String token = tokenFor("l3@hakku.dev");

        mvc.perform(post("/api/posts/99999/likes").header("Authorization", token))
                .andExpect(status().isNotFound());
    }
}
