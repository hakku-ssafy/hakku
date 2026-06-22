package com.hakku.main.cart.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hakku.main.cart.domain.CartItem;
import com.hakku.main.product.domain.Product;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * CartItemRepository(MyBatis 매퍼) 통합 테스트.
 * H2 스키마는 Flyway 마이그레이션(실제 운영 스키마)으로 제공되며 FK 제약을 강제한다.
 * 따라서 장바구니 항목을 넣기 전에 참조 부모 행(users, products)을 먼저 생성하고 생성된 실제 id 를 사용한다.
 */
@SpringBootTest
@Transactional
class CartItemRepositoryMapperTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private Long userId;
    private Long productId;

    @BeforeEach
    void setUp() {
        User seller = userRepository.save(new User("cart-seller@test.com", "판매자", Role.SELLER));
        User buyer = userRepository.save(new User("cart-buyer@test.com", "구매자", Role.NORMAL));
        userId = buyer.getId();
        Product product = productRepository.save(new Product(
                seller.getId(), "상품", "설명", 10000L,
                "TOP", "WINTER", "SUMMER", null, null, null, null));
        productId = product.getId();
    }

    @Test
    @DisplayName("save(신규) 후 회원-상품/본인 항목으로 조회되고 타임스탬프가 채워진다")
    void insert_thenFind() {
        CartItem saved = cartItemRepository.save(new CartItem(userId, productId, 2));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(cartItemRepository.findByUserIdAndProductId(userId, productId)).isPresent();
        assertThat(cartItemRepository.findByIdAndUserId(saved.getId(), userId)).isPresent();
        assertThat(cartItemRepository.findByIdAndUserId(saved.getId(), 999L)).isEmpty();
        assertThat(cartItemRepository.findByUserId(userId)).hasSize(1);
    }

    @Test
    @DisplayName("save(갱신) 시 수량이 변경된다")
    void update_changesQuantity() {
        CartItem saved = cartItemRepository.save(new CartItem(userId, productId, 1));

        saved.changeQuantity(5);
        cartItemRepository.save(saved);

        Optional<CartItem> reloaded = cartItemRepository.findByIdAndUserId(saved.getId(), userId);
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("delete 후 존재하지 않는다")
    void delete_thenAbsent() {
        CartItem saved = cartItemRepository.save(new CartItem(userId, productId, 1));

        cartItemRepository.delete(saved);

        assertThat(cartItemRepository.findByIdAndUserId(saved.getId(), userId)).isEmpty();
        assertThat(cartItemRepository.findByUserId(userId)).isEmpty();
    }
}
