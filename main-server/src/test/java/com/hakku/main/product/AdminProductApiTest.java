package com.hakku.main.product;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 어드민 상품 관리 API — 커서 페이지네이션 + 인라인 수정(활성화/태그/이름).
 * ADMIN 계정은 AdminSeeder(@TestPropertySource 로 설정)로 시드한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.admin.email=admin@hakku.local",
        "app.admin.password=admin1234",
        "app.admin.nickname=관리자"
})
class AdminProductApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // @SpringBootTest 컨텍스트는 인메모리 H2 를 메서드 간 공유한다(트랜잭션 롤백 없음).
    // 커서/공개목록 단언이 다른 메서드의 잔여 상품에 오염되지 않도록 매번 비운다.
    // H2 테스트 스키마 FK 에 ON DELETE CASCADE 가 없어 자식 테이블부터 지운다.
    @BeforeEach
    void resetProducts() {
        jdbc.update("DELETE FROM product_styles");
        jdbc.update("DELETE FROM product_colors");
        jdbc.update("DELETE FROM products");
    }

    private String adminToken() throws Exception {
        return login("admin@hakku.local", "admin1234");
    }

    private String sellerToken(String email) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"secret123","nickname":"판매자","role":"SELLER"}
                    """.formatted(email)));
        return login(email, "secret123");
    }

    private String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","password":"%s"}
                            """.formatted(email, password)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    private long createProduct(String token, String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/products")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"%s","description":"d","price":1000,"category":"키링",
                             "colors":[],"styles":["기본"],"imageUrl":"/product-images/x.jpg"}
                            """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    @DisplayName("어드민 상품 목록은 커서 페이지네이션(hasMore/nextCursor)을 지원한다")
    void adminListIsCursorPaginated() throws Exception {
        String admin = adminToken();
        long p1 = createProduct(admin, "상품1");
        long p2 = createProduct(admin, "상품2");
        long p3 = createProduct(admin, "상품3");

        // 첫 페이지: limit=2 → 최신순 [p3, p2], hasMore=true, nextCursor=p2
        MvcResult page1 = mvc.perform(get("/api/admin/products?limit=2").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(p3))
                .andExpect(jsonPath("$.items[1].id").value(p2))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").value(p2))
                .andReturn();
        JsonNode cursor = objectMapper.readTree(page1.getResponse().getContentAsString()).get("nextCursor");

        // 둘째 페이지: cursorId=p2 → [p1], hasMore=false, nextCursor=null
        mvc.perform(get("/api/admin/products?limit=2&cursorId=" + cursor.asLong()).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(p1))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    @DisplayName("어드민이 비활성화한 상품은 공개 목록에서 숨고, 어드민 목록엔 남는다")
    void deactivatedProductHiddenFromPublic() throws Exception {
        String admin = adminToken();
        long id = createProduct(admin, "숨길상품");

        mvc.perform(patch("/api/admin/products/" + id)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"숨길상품","category":"키링","styles":["기본"],"active":false}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // 공개 목록: 비활성 상품 제외
        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").doesNotExist());

        // 어드민 목록: 비활성 상품 포함
        mvc.perform(get("/api/admin/products?limit=50").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + id + ")].active").value(false));
    }

    @Test
    @DisplayName("어드민은 이름·카테고리·태그(styles)를 인라인 수정할 수 있다")
    void adminEditsNameCategoryTagsInline() throws Exception {
        String admin = adminToken();
        long id = createProduct(admin, "원래이름");

        mvc.perform(patch("/api/admin/products/" + id)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"바뀐이름","category":"핀뱃지","styles":["빈티지","러블리"],"active":true}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("바뀐이름"))
                .andExpect(jsonPath("$.category").value("핀뱃지"))
                .andExpect(jsonPath("$.styles").isArray())
                .andExpect(jsonPath("$.styles", org.hamcrest.Matchers.containsInAnyOrder("빈티지", "러블리")));
    }

    @Test
    @DisplayName("ADMIN 이 아니면 어드민 상품 API 는 403")
    void nonAdminForbidden() throws Exception {
        String seller = sellerToken("seller-x@hakku.local");
        mvc.perform(get("/api/admin/products").header("Authorization", seller))
                .andExpect(status().isForbidden());
    }
}
