package com.hakku.main.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class OrderApiTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ADDRESS = """
            "recipientName":"홍길동","phone":"010-1234-5678","postalCode":"06236",
            "address1":"서울 강남구 테헤란로 1","address2":"101동 1001호"
            """;

    private String tokenFor(String email, String role) throws Exception {
        mvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","password":"secret123","nickname":"주문자","role":"%s"}
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
                            {"name":"키링","description":"다꾸 키링","price":4900,
                             "keyColor":"WINTER","subColor":null,"styles":["미니멀"],
                             "imageUrl":"https://img/keyring.png"}
                            """))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private void addToCart(String token, long productId, int qty) throws Exception {
        mvc.perform(post("/api/cart/items")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"productId":%d,"quantity":%d}
                            """.formatted(productId, qty)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("장바구니 → 주문 생성 201, 주문 내역 조회 200 (항목/합계 포함)")
    void createAndList() throws Exception {
        long productId = createProduct(tokenFor("os1@hakku.dev", "SELLER"));
        String buyer = tokenFor("ob1@hakku.dev", "NORMAL");
        addToCart(buyer, productId, 2);

        mvc.perform(post("/api/orders")
                        .header("Authorization", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{%s}".formatted(ADDRESS)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(9800))
                .andExpect(jsonPath("$.recipientName").value("홍길동"))
                .andExpect(jsonPath("$.items[0].productName").value("키링"))
                .andExpect(jsonPath("$.items[0].lineTotal").value(9800));

        mvc.perform(get("/api/orders").header("Authorization", buyer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalAmount").value(9800))
                .andExpect(jsonPath("$[0].items[0].productName").value("키링"));
    }

    @Test
    @DisplayName("빈 장바구니로 주문 → 400")
    void emptyCart() throws Exception {
        String buyer = tokenFor("ob2@hakku.dev", "NORMAL");

        mvc.perform(post("/api/orders")
                        .header("Authorization", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{%s}".formatted(ADDRESS)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("필수 주소 누락 → 400")
    void missingAddress() throws Exception {
        long productId = createProduct(tokenFor("os3@hakku.dev", "SELLER"));
        String buyer = tokenFor("ob3@hakku.dev", "NORMAL");
        addToCart(buyer, productId, 1);

        mvc.perform(post("/api/orders")
                        .header("Authorization", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"recipientName":"","phone":"","postalCode":"","address1":""}
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 없이 주문 → 401")
    void noToken() throws Exception {
        mvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{%s}".formatted(ADDRESS)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("타인 주문 단건 조회 → 404 (존재 노출 방지)")
    void othersOrder() throws Exception {
        long productId = createProduct(tokenFor("os4@hakku.dev", "SELLER"));
        String owner = tokenFor("ob4@hakku.dev", "NORMAL");
        addToCart(owner, productId, 1);
        String body = mvc.perform(post("/api/orders")
                        .header("Authorization", owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{%s}".formatted(ADDRESS)))
                .andReturn().getResponse().getContentAsString();
        long orderId = objectMapper.readTree(body).get("id").asLong();

        String other = tokenFor("ob5@hakku.dev", "NORMAL");
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", other))
                .andExpect(status().isNotFound());
    }
}
