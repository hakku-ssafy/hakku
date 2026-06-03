package com.hakku.main.cart;

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
class CartApiTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenFor(String email, String role) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"secret123","nickname":"구매자","role":"%s"}
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

    private long addToCart(String token, long productId, int qty) throws Exception {
        String body = mvc.perform(post("/api/cart/items")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"productId":%d,"quantity":%d}
                            """.formatted(productId, qty)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @DisplayName("담기 → 201, 목록 → 200 (라인합계 포함)")
    void addAndList() throws Exception {
        String seller = tokenFor("cs1@hakku.dev", "SELLER");
        long productId = createProduct(seller);
        String buyer = tokenFor("cb1@hakku.dev", "NORMAL");

        addToCart(buyer, productId, 2);

        mvc.perform(get("/api/cart/items").header("Authorization", buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].lineTotal").value(178000));
    }

    @Test
    @DisplayName("같은 상품 다시 담기 → 수량 누적")
    void addSameProductTwice() throws Exception {
        long productId = createProduct(tokenFor("cs2@hakku.dev", "SELLER"));
        String buyer = tokenFor("cb2@hakku.dev", "NORMAL");

        addToCart(buyer, productId, 1);
        mvc.perform(post("/api/cart/items")
                        .header("Authorization", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"productId":%d,"quantity":2}
                            """.formatted(productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    @DisplayName("없는 상품 담기 → 404")
    void addMissingProduct() throws Exception {
        String buyer = tokenFor("cb3@hakku.dev", "NORMAL");

        mvc.perform(post("/api/cart/items")
                        .header("Authorization", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"productId":99999,"quantity":1}
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("담기 토큰 없음 → 401")
    void addWithoutToken() throws Exception {
        mvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"productId":1,"quantity":1}
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("수량 변경 → 200")
    void updateQuantity() throws Exception {
        long productId = createProduct(tokenFor("cs4@hakku.dev", "SELLER"));
        String buyer = tokenFor("cb4@hakku.dev", "NORMAL");
        long itemId = addToCart(buyer, productId, 1);

        mvc.perform(put("/api/cart/items/" + itemId)
                        .header("Authorization", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"quantity":7}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(7));
    }

    @Test
    @DisplayName("타인 장바구니 항목 변경 → 404 (존재 노출 방지)")
    void updateOthersItem() throws Exception {
        long productId = createProduct(tokenFor("cs5@hakku.dev", "SELLER"));
        String owner = tokenFor("cb5@hakku.dev", "NORMAL");
        long itemId = addToCart(owner, productId, 1);
        String other = tokenFor("cb6@hakku.dev", "NORMAL");

        mvc.perform(put("/api/cart/items/" + itemId)
                        .header("Authorization", other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"quantity":9}
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("항목 삭제 → 204")
    void removeItem() throws Exception {
        long productId = createProduct(tokenFor("cs7@hakku.dev", "SELLER"));
        String buyer = tokenFor("cb7@hakku.dev", "NORMAL");
        long itemId = addToCart(buyer, productId, 1);

        mvc.perform(delete("/api/cart/items/" + itemId).header("Authorization", buyer))
                .andExpect(status().isNoContent());
    }
}
