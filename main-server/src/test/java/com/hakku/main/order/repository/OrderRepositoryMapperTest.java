package com.hakku.main.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hakku.main.cart.domain.CartItem;
import com.hakku.main.cart.repository.CartItemRepository;
import com.hakku.main.order.domain.Order;
import com.hakku.main.order.domain.OrderItem;
import com.hakku.main.order.domain.OrderStatus;
import com.hakku.main.order.domain.ShippingAddress;
import com.hakku.main.product.domain.Product;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * OrderRepository(MyBatis 매퍼) 통합 테스트. H2 스키마는 Flyway 마이그레이션으로 제공된다.
 */
@SpringBootTest
@Transactional
class OrderRepositoryMapperTest {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CartItemRepository cartItemRepository;

    private Long userId;
    private Long productId;

    @BeforeEach
    void setUp() {
        User seller = userRepository.save(new User("order-seller@test.com", "판매자", Role.SELLER));
        User buyer = userRepository.save(new User("order-buyer@test.com", "구매자", Role.NORMAL));
        userId = buyer.getId();
        Product product = productRepository.save(new Product(
                seller.getId(), "키링", "다꾸 키링", 4900L,
                "키링", "WINTER", null, null, null, null, null));
        productId = product.getId();
    }

    private ShippingAddress address() {
        return new ShippingAddress("홍길동", "010-1234-5678", "06236", "서울 강남구 테헤란로 1", "101동 1001호");
    }

    private Order newOrder() {
        return new Order(userId, address(), List.of(new OrderItem(productId, "키링", 4900L, 2)));
    }

    @Test
    @DisplayName("save(신규) 후 회원 주문 목록으로 항목과 함께 조회된다")
    void insert_thenFindByUser() {
        Order saved = orderRepository.save(newOrder());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        List<Order> orders = orderRepository.findByUserId(userId);
        assertThat(orders).hasSize(1);
        Order loaded = orders.get(0);
        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(loaded.getRecipientName()).isEqualTo("홍길동");
        assertThat(loaded.getTotalAmount()).isEqualTo(9800L);
        assertThat(loaded.getItems()).hasSize(1);
        assertThat(loaded.getItems().get(0).getProductName()).isEqualTo("키링");
        assertThat(loaded.getItems().get(0).getLineTotal()).isEqualTo(9800L);
    }

    @Test
    @DisplayName("selectByIdAndUserId: 본인 주문만 조회된다")
    void findByIdAndUser_ownershipScoped() {
        Order saved = orderRepository.save(newOrder());

        assertThat(orderRepository.findByIdAndUserId(saved.getId(), userId)).isPresent();
        assertThat(orderRepository.findByIdAndUserId(saved.getId(), 999L)).isEmpty();
    }

    @Test
    @DisplayName("updateStatus: PAID 전이가 영속된다")
    void updateStatus_persists() {
        Order saved = orderRepository.save(newOrder());

        saved.markPaid();
        orderRepository.save(saved);

        Order reloaded = orderRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("CartItemRepository.deleteByUserId: 회원 장바구니를 모두 비운다")
    void cart_deleteByUserId_clears() {
        cartItemRepository.save(new CartItem(userId, productId, 1));
        assertThat(cartItemRepository.findByUserId(userId)).hasSize(1);

        cartItemRepository.deleteByUserId(userId);

        assertThat(cartItemRepository.findByUserId(userId)).isEmpty();
    }
}
