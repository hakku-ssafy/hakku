package com.hakku.main.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.main.cart.domain.CartItem;
import com.hakku.main.cart.exception.CartItemNotFoundException;
import com.hakku.main.cart.repository.CartItemRepository;
import com.hakku.main.product.domain.Product;
import com.hakku.main.product.exception.ProductNotFoundException;
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
class CartServiceTest {

    private static final Long USER = 1L;
    private static final Long PRODUCT = 10L;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartItemRepository, productRepository);
    }

    private Product product() {
        return new Product(2L, "코트", "겨울 코트", 89000L, "기타", "WINTER", null, Set.of(), Set.of("미니멀"), "https://img/coat.png", null);
    }

    @Test
    @DisplayName("addItem: 새 상품이면 수량과 함께 담고 상품 정보를 응답한다")
    void addItem_new_adds() {
        when(productRepository.findById(PRODUCT)).thenReturn(Optional.of(product()));
        when(cartItemRepository.findByUserIdAndProductId(USER, PRODUCT)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItemResponse response = cartService.addItem(USER, PRODUCT, 2);

        assertThat(response.productId()).isEqualTo(PRODUCT);
        assertThat(response.quantity()).isEqualTo(2);
        assertThat(response.productName()).isEqualTo("코트");
        assertThat(response.price()).isEqualTo(89000L);
        assertThat(response.lineTotal()).isEqualTo(178000L);
    }

    @Test
    @DisplayName("addItem: 이미 담은 상품이면 수량을 증가시킨다")
    void addItem_existing_increments() {
        CartItem existing = new CartItem(USER, PRODUCT, 1);
        when(productRepository.findById(PRODUCT)).thenReturn(Optional.of(product()));
        when(cartItemRepository.findByUserIdAndProductId(USER, PRODUCT)).thenReturn(Optional.of(existing));

        CartItemResponse response = cartService.addItem(USER, PRODUCT, 3);

        assertThat(response.quantity()).isEqualTo(4);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("addItem: 없는 상품이면 ProductNotFoundException")
    void addItem_missingProduct_throws() {
        when(productRepository.findById(PRODUCT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(USER, PRODUCT, 1))
                .isInstanceOf(ProductNotFoundException.class);
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("list: 회원의 장바구니 항목을 상품 정보와 함께 반환한다")
    void list_returnsItems() {
        CartItem item = new CartItem(USER, PRODUCT, 2);
        when(cartItemRepository.findByUserId(USER)).thenReturn(List.of(item));
        when(productRepository.findById(PRODUCT)).thenReturn(Optional.of(product()));

        List<CartItemResponse> items = cartService.list(USER);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).lineTotal()).isEqualTo(178000L);
    }

    @Test
    @DisplayName("updateQuantity: 본인 항목이면 수량을 변경한다")
    void updateQuantity_owner_updates() {
        CartItem item = new CartItem(USER, PRODUCT, 1);
        when(cartItemRepository.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(item));
        when(productRepository.findById(PRODUCT)).thenReturn(Optional.of(product()));

        CartItemResponse response = cartService.updateQuantity(USER, 5L, 5);

        assertThat(response.quantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("updateQuantity: 본인 항목이 아니거나 없으면 CartItemNotFoundException")
    void updateQuantity_notOwner_throws() {
        when(cartItemRepository.findByIdAndUserId(5L, USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateQuantity(USER, 5L, 3))
                .isInstanceOf(CartItemNotFoundException.class);
    }

    @Test
    @DisplayName("removeItem: 본인 항목이면 삭제한다")
    void removeItem_owner_deletes() {
        CartItem item = new CartItem(USER, PRODUCT, 1);
        when(cartItemRepository.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(item));

        cartService.removeItem(USER, 5L);

        verify(cartItemRepository).delete(item);
    }
}
