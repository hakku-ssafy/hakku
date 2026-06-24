package com.hakku.main.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.main.cart.domain.CartItem;
import com.hakku.main.cart.repository.CartItemRepository;
import com.hakku.main.order.domain.Order;
import com.hakku.main.order.domain.OrderItem;
import com.hakku.main.order.domain.OrderStatus;
import com.hakku.main.order.domain.ShippingAddress;
import com.hakku.main.order.exception.EmptyCartException;
import com.hakku.main.order.exception.OrderNotFoundException;
import com.hakku.main.order.repository.OrderRepository;
import com.hakku.main.product.domain.Product;
import com.hakku.main.product.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Long USER = 1L;
    private static final Long PRODUCT = 10L;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, cartItemRepository, productRepository);
    }

    private Product product() {
        return new Product(2L, "키링", "다꾸 키링", 4900L, "키링", "WINTER", null,
                Set.of(), Set.of("미니멀"), "https://img/keyring.png", null);
    }

    private ShippingAddress address() {
        return new ShippingAddress("홍길동", "010-1234-5678", "06236", "서울 강남구 테헤란로 1", "101동 1001호");
    }

    @Test
    @DisplayName("createFromCart: 장바구니 항목을 스냅샷해 주문을 만들고 합계/주소/CREATED 상태를 반환한다")
    void createFromCart_snapshotsCart() {
        when(cartItemRepository.findByUserId(USER)).thenReturn(List.of(new CartItem(USER, PRODUCT, 2)));
        when(productRepository.findById(PRODUCT)).thenReturn(Optional.of(product()));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.createFromCart(USER, address());

        assertThat(response.status()).isEqualTo("CREATED");
        assertThat(response.recipientName()).isEqualTo("홍길동");
        assertThat(response.totalAmount()).isEqualTo(9800L); // 4900 * 2
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("키링");
        assertThat(response.items().get(0).lineTotal()).isEqualTo(9800L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("createFromCart: 빈 장바구니면 EmptyCartException, 저장하지 않는다")
    void createFromCart_emptyCart_throws() {
        when(cartItemRepository.findByUserId(USER)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.createFromCart(USER, address()))
                .isInstanceOf(EmptyCartException.class);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("listMyOrders: 회원의 주문 목록을 반환한다")
    void listMyOrders_returns() {
        Order order = new Order(USER, address(), List.of(new OrderItem(PRODUCT, "키링", 4900L, 2)));
        when(orderRepository.findByUserId(USER)).thenReturn(List.of(order));

        List<OrderResponse> orders = orderService.listMyOrders(USER);

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).totalAmount()).isEqualTo(9800L);
    }

    @Test
    @DisplayName("get: 본인 주문이 아니거나 없으면 OrderNotFoundException")
    void get_missing_throws() {
        when(orderRepository.findByIdAndUserId(99L, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.get(USER, 99L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("markPaid: 주문을 PAID 로 전이하고 저장하며 장바구니를 비우고 true 를 반환한다")
    void markPaid_marksAndClearsCart() {
        Order order = new Order(USER, address(), List.of(new OrderItem(PRODUCT, "키링", 4900L, 2)));
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        boolean transitioned = orderService.markPaid(7L);

        assertThat(transitioned).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
        verify(cartItemRepository).deleteByUserId(USER);
    }

    @Test
    @DisplayName("markPaid: 이미 PAID 면 멱등 no-op 으로 false 를 반환한다(이벤트 중복 수신 대비)")
    void markPaid_alreadyPaid_returnsFalse() {
        Order order = new Order(USER, address(), List.of(new OrderItem(PRODUCT, "키링", 4900L, 2)));
        order.markPaid();
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        boolean transitioned = orderService.markPaid(7L);

        assertThat(transitioned).isFalse();
        verify(orderRepository, never()).save(any(Order.class));
        verify(cartItemRepository, never()).deleteByUserId(any());
    }
}
