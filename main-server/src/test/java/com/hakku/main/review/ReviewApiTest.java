package com.hakku.main.review;

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
class ReviewApiTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenFor(String email, String role) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"secret123","nickname":"리뷰어","role":"%s"}
                    """.formatted(email, role)));
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","password":"secret123"}
                            """.formatted(email)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    private long createProduct(String sellerToken) throws Exception {
        String body = mvc.perform(post("/api/products")
                        .header("Authorization", sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"코트","description":"겨울 코트","price":89000,
                             "keyColor":"WINTER","subColor":null,"styles":["미니멀"],
                             "imageUrl":"https://img/coat.png"}
                            """))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long createReview(String token, long productId, int rating) throws Exception {
        String body = mvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"rating":%d,"content":"만족합니다"}
                            """.formatted(rating)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("리뷰 작성 → 201, 목록(공개) → 200")
    void createAndList() throws Exception {
        long productId = createProduct(tokenFor("rs1@hakku.dev", "SELLER"));
        String reviewer = tokenFor("rr1@hakku.dev", "NORMAL");
        createReview(reviewer, productId, 5);

        mvc.perform(get("/api/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].content").value("만족합니다"));
    }

    @Test
    @DisplayName("같은 상품에 두 번째 리뷰 → 409")
    void duplicateReview() throws Exception {
        long productId = createProduct(tokenFor("rs2@hakku.dev", "SELLER"));
        String reviewer = tokenFor("rr2@hakku.dev", "NORMAL");
        createReview(reviewer, productId, 4);

        mvc.perform(post("/api/products/" + productId + "/reviews")
                        .header("Authorization", reviewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"rating":3,"content":"또 작성"}
                            """))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("없는 상품에 리뷰 → 404")
    void reviewMissingProduct() throws Exception {
        String reviewer = tokenFor("rr3@hakku.dev", "NORMAL");

        mvc.perform(post("/api/products/99999/reviews")
                        .header("Authorization", reviewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"rating":5,"content":"유령 상품"}
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("리뷰 작성 토큰 없음 → 401")
    void reviewWithoutToken() throws Exception {
        long productId = createProduct(tokenFor("rs4@hakku.dev", "SELLER"));

        mvc.perform(post("/api/products/" + productId + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"rating":5,"content":"x"}
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("작성자 본인 수정 → 200")
    void updateByAuthor() throws Exception {
        long productId = createProduct(tokenFor("rs5@hakku.dev", "SELLER"));
        String reviewer = tokenFor("rr5@hakku.dev", "NORMAL");
        long reviewId = createReview(reviewer, productId, 5);

        mvc.perform(put("/api/reviews/" + reviewId)
                        .header("Authorization", reviewer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"rating":2,"content":"생각보다 별로"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(2));
    }

    @Test
    @DisplayName("타인 리뷰 수정 → 403")
    void updateByOther() throws Exception {
        long productId = createProduct(tokenFor("rs6@hakku.dev", "SELLER"));
        String author = tokenFor("rr6@hakku.dev", "NORMAL");
        long reviewId = createReview(author, productId, 5);
        String other = tokenFor("rr7@hakku.dev", "NORMAL");

        mvc.perform(put("/api/reviews/" + reviewId)
                        .header("Authorization", other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"rating":1,"content":"가로채기"}
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("작성자 본인 삭제 → 204")
    void deleteByAuthor() throws Exception {
        long productId = createProduct(tokenFor("rs8@hakku.dev", "SELLER"));
        String reviewer = tokenFor("rr8@hakku.dev", "NORMAL");
        long reviewId = createReview(reviewer, productId, 4);

        mvc.perform(delete("/api/reviews/" + reviewId).header("Authorization", reviewer))
                .andExpect(status().isNoContent());
    }
}
