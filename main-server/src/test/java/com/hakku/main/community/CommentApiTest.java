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
class CommentApiTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenFor(String email) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"secret123","nickname":"댓글러","role":"NORMAL"}
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
                            {"title":"댓글 대상","content":"본문"}
                            """))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long createComment(String token, long postId, String content) throws Exception {
        String body = mvc.perform(post("/api/posts/" + postId + "/comments")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"content":"%s"}
                            """.formatted(content)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("POST 댓글 → 201, GET 목록(공개) → 200")
    void createAndList() throws Exception {
        String token = tokenFor("c1@hakku.dev");
        long postId = createPost(token);
        createComment(token, postId, "첫 댓글");

        mvc.perform(get("/api/posts/" + postId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("첫 댓글"));
    }

    @Test
    @DisplayName("POST 댓글 토큰 없음 → 401")
    void createWithoutToken() throws Exception {
        long postId = createPost(tokenFor("c2@hakku.dev"));

        mvc.perform(post("/api/posts/" + postId + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"content":"x"}
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST 댓글: 없는 게시글 → 404")
    void createOnMissingPost() throws Exception {
        String token = tokenFor("c3@hakku.dev");

        mvc.perform(post("/api/posts/99999/comments")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"content":"유령 게시글"}
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT 댓글 타인 → 403")
    void updateByOther() throws Exception {
        String owner = tokenFor("c4@hakku.dev");
        long postId = createPost(owner);
        long commentId = createComment(owner, postId, "내 댓글");
        String other = tokenFor("c5@hakku.dev");

        mvc.perform(put("/api/comments/" + commentId)
                        .header("Authorization", other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"content":"가로채기"}
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE 댓글 작성자 본인 → 204")
    void deleteByAuthor() throws Exception {
        String token = tokenFor("c6@hakku.dev");
        long postId = createPost(token);
        long commentId = createComment(token, postId, "삭제될 댓글");

        mvc.perform(delete("/api/comments/" + commentId).header("Authorization", token))
                .andExpect(status().isNoContent());
    }
}
