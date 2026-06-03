package com.hakku.main.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.main.product.domain.Product;
import com.hakku.main.product.exception.ProductAccessDeniedException;
import com.hakku.main.product.exception.ProductNotFoundException;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.user.domain.Role;
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
class ProductServiceTest {

    private static final Long SELLER = 1L;
    private static final Long OTHER_SELLER = 2L;

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    private ProductCommand command() {
        return new ProductCommand("코트", "겨울 코트", 89000L, "WINTER", null,
                Set.of("미니멀", "클래식"), "https://img/coat.png");
    }

    @Test
    @DisplayName("create: SELLER 가 등록하면 상품을 저장한다")
    void create_bySeller_saves() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.create(SELLER, Role.SELLER, command());

        assertThat(response.name()).isEqualTo("코트");
        assertThat(response.sellerId()).isEqualTo(SELLER);
        assertThat(response.price()).isEqualTo(89000L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("create: 일반 회원(NORMAL)은 등록할 수 없다 → ProductAccessDeniedException")
    void create_byNormal_denied() {
        assertThatThrownBy(() -> productService.create(SELLER, Role.NORMAL, command()))
                .isInstanceOf(ProductAccessDeniedException.class);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("get: 없는 상품이면 ProductNotFoundException")
    void get_missing_throws() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.get(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("update: 등록한 판매자 본인이면 갱신한다")
    void update_byOwner_updates() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product()));

        ProductCommand changed = new ProductCommand("니트", "봄 니트", 49000L, "SPRING", null,
                Set.of("캐주얼"), "https://img/knit.png");
        ProductResponse response = productService.update(1L, SELLER, changed);

        assertThat(response.name()).isEqualTo("니트");
        assertThat(response.price()).isEqualTo(49000L);
    }

    @Test
    @DisplayName("update: 다른 판매자가 수정하면 ProductAccessDeniedException")
    void update_byOther_denied() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product()));

        assertThatThrownBy(() -> productService.update(1L, OTHER_SELLER, command()))
                .isInstanceOf(ProductAccessDeniedException.class);
    }

    @Test
    @DisplayName("delete: 다른 판매자가 삭제하면 거부하고 삭제하지 않는다")
    void delete_byOther_denied() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product()));

        assertThatThrownBy(() -> productService.delete(1L, OTHER_SELLER))
                .isInstanceOf(ProductAccessDeniedException.class);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    @DisplayName("delete: 등록한 판매자 본인이면 삭제한다")
    void delete_byOwner_deletes() {
        Product product = product();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.delete(1L, SELLER);

        verify(productRepository).delete(product);
    }

    @Test
    @DisplayName("list: 모든 상품을 응답으로 변환한다")
    void list_returnsAll() {
        when(productRepository.findAllByOrderByIdDesc()).thenReturn(List.of(product(), product()));

        List<ProductResponse> all = productService.list();

        assertThat(all).hasSize(2);
    }

    private Product product() {
        return new Product(SELLER, "코트", "겨울 코트", 89000L, "WINTER", null,
                Set.of("미니멀", "클래식"), "https://img/coat.png");
    }
}
