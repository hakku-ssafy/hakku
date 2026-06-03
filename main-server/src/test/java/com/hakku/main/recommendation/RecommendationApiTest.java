package com.hakku.main.recommendation;

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

/**
 * 추천 목록 API 통합 테스트. {@code GET /api/recommendations} 는 인증 회원에게
 * 선호 신호에 맞춰 정렬된 추천 상품을 반환한다.
 *
 * <p>테스트 DB(H2)는 컨텍스트 공유로 다른 테스트의 상품이 누적될 수 있으므로, 다른 곳에서
 * 쓰지 않는 고유 스타일 태그로 대상 상품만 점수를 획득하게 해 정렬 결과를 결정적으로 만든다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RecommendationApiTest {

    /** 다른 테스트가 사용하지 않는 고유 스타일 → 이 스타일을 가진 상품만 style 점수를 얻는다. */
    private static final String UNIQUE_STYLE = "추천전용스타일";

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenFor(String email, String role) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"secret123","nickname":"사용자","role":"%s"}
                    """.formatted(email, role)));
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","password":"secret123"}
                            """.formatted(email)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    private void createProduct(String sellerToken, String name, String stylesJson) throws Exception {
        mvc.perform(post("/api/products")
                        .header("Authorization", sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"%s","description":"d","price":10000,
                             "keyColor":null,"subColor":null,"styles":%s,"imageUrl":null}
                            """.formatted(name, stylesJson)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("인증 회원 추천 목록 → 200, 선호 스타일이 맞는 상품이 맨 앞")
    void recommendForUser() throws Exception {
        String seller = tokenFor("rec-seller@hakku.dev", "SELLER");
        createProduct(seller, "추천대상", "[\"" + UNIQUE_STYLE + "\"]");
        createProduct(seller, "비대상", "[\"무관스타일\"]");

        String user = tokenFor("rec-user@hakku.dev", "NORMAL");
        mvc.perform(put("/api/users/me")
                        .header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"nickname":"회원","profileImageUrl":null,"preferredStyles":["%s"]}
                            """.formatted(UNIQUE_STYLE)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/recommendations").header("Authorization", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].product.name").value("추천대상"))
                .andExpect(jsonPath("$[0].score").value(2.0))
                .andExpect(jsonPath("$[0].breakdown.style").value(2.0));
    }

    @Test
    @DisplayName("토큰 없이 추천 → 401")
    void recommendWithoutToken() throws Exception {
        mvc.perform(get("/api/recommendations"))
                .andExpect(status().isUnauthorized());
    }
}
