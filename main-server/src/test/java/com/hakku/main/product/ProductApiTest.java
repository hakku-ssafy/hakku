package com.hakku.main.product;

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
class ProductApiTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenFor(String email, String role) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"secret123","nickname":"판매자","role":"%s"}
                    """.formatted(email, role)));
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"%s","password":"secret123"}
                            """.formatted(email)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + objectMapper.readTree(body).get("accessToken").asText();
    }

    private static final String PRODUCT_JSON = """
        {"name":"코트","description":"겨울 코트","price":89000,
         "keyColor":"WINTER","subColor":null,"styles":["미니멀","클래식"],
         "imageUrl":"https://img/coat.png"}
        """;

    private long createProduct(String sellerToken) throws Exception {
        String body = mvc.perform(post("/api/products")
                        .header("Authorization", sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("SELLER 상품 등록 → 201, GET 단건(공개) → 200")
    void createAndGet() throws Exception {
        String seller = tokenFor("seller1@hakku.dev", "SELLER");
        long id = createProduct(seller);

        mvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("코트"))
                .andExpect(jsonPath("$.price").value(89000));
    }

    @Test
    @DisplayName("GET 목록(공개) → 200")
    void listPublic() throws Exception {
        createProduct(tokenFor("seller2@hakku.dev", "SELLER"));

        mvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    @DisplayName("일반 회원(NORMAL) 상품 등록 → 403")
    void createByNormal() throws Exception {
        String normal = tokenFor("normal1@hakku.dev", "NORMAL");

        mvc.perform(post("/api/products")
                        .header("Authorization", normal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("토큰 없이 상품 등록 → 401")
    void createWithoutToken() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("등록한 판매자 본인 수정 → 200")
    void updateByOwner() throws Exception {
        String seller = tokenFor("seller3@hakku.dev", "SELLER");
        long id = createProduct(seller);

        mvc.perform(put("/api/products/" + id)
                        .header("Authorization", seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name":"니트","description":"봄 니트","price":49000,
                             "keyColor":"SPRING","subColor":null,"styles":["캐주얼"],
                             "imageUrl":"https://img/knit.png"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("니트"));
    }

    @Test
    @DisplayName("다른 판매자 수정 → 403")
    void updateByOther() throws Exception {
        long id = createProduct(tokenFor("seller4@hakku.dev", "SELLER"));
        String other = tokenFor("seller5@hakku.dev", "SELLER");

        mvc.perform(put("/api/products/" + id)
                        .header("Authorization", other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCT_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("등록한 판매자 본인 삭제 → 204")
    void deleteByOwner() throws Exception {
        String seller = tokenFor("seller6@hakku.dev", "SELLER");
        long id = createProduct(seller);

        mvc.perform(delete("/api/products/" + id).header("Authorization", seller))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/products/" + id))
                .andExpect(status().isNotFound());
    }
}
