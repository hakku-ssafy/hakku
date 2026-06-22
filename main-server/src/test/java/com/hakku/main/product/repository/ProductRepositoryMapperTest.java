package com.hakku.main.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hakku.main.product.domain.Product;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * ProductRepository(MyBatis 매퍼) 통합 테스트. @ElementCollection(색상/스타일) 라운드트립 포함.
 * H2 스키마는 Flyway 마이그레이션(실제 운영 스키마)으로 제공되며 FK 제약을 강제한다.
 * 따라서 상품을 넣기 전에 참조 부모 행(users)을 먼저 생성하고 생성된 실제 id 를 판매자로 사용한다.
 */
@SpringBootTest
@Transactional
class ProductRepositoryMapperTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private Long sellerId;

    @BeforeEach
    void setUp() {
        sellerId = userRepository.save(new User("product-seller@test.com", "판매자", Role.SELLER)).getId();
    }

    private Product newProduct(String name, Set<String> colors, Set<String> styles) {
        return new Product(sellerId, name, "설명", 10000L, "기타",
                "WINTER", "SUMMER", colors, styles, "http://img", "http://buy");
    }

    @Test
    @DisplayName("save(신규) 후 단건 조회되고 컬렉션/타임스탬프가 채워진다")
    void insert_thenFindWithCollections() {
        Product saved = productRepository.save(
                newProduct("코트", Set.of("WINTER", "BLACK"), Set.of("미니멀")));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        Product found = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("코트");
        assertThat(found.getKeyColor()).isEqualTo("WINTER");
        assertThat(found.getColors()).containsExactlyInAnyOrder("WINTER", "BLACK");
        assertThat(found.getStyles()).containsExactly("미니멀");
        assertThat(productRepository.existsById(saved.getId())).isTrue();
        assertThat(productRepository.existsById(999999L)).isFalse();
    }

    @Test
    @DisplayName("findAllByOrderByIdDesc 는 최신순으로 반환한다")
    void findAllByOrderByIdDesc() {
        Product older = productRepository.save(newProduct("a", Set.of(), Set.of()));
        Product newer = productRepository.save(newProduct("b", Set.of(), Set.of()));

        // 다른 테스트가 남긴 행이 있을 수 있으므로(@SpringBootTest API 테스트 비롤백) 상대 순서만 검증한다.
        assertThat(productRepository.findAllByOrderByIdDesc())
                .extracting(Product::getId)
                .containsSubsequence(newer.getId(), older.getId());
    }

    @Test
    @DisplayName("save(갱신) 시 필드와 컬렉션이 delete 후 재삽입된다")
    void update_replacesFieldsAndCollections() {
        Product saved = productRepository.save(
                newProduct("old", Set.of("WINTER"), Set.of("올드")));

        saved.update("new", "새설명", 20000L, "상의", "SUMMER", "SPRING",
                Set.of("SUMMER", "WHITE"), Set.of("뉴"), "http://new", "http://newbuy");
        productRepository.save(saved);

        Product reloaded = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("new");
        assertThat(reloaded.getPrice()).isEqualTo(20000L);
        assertThat(reloaded.getColors()).containsExactlyInAnyOrder("SUMMER", "WHITE");
        assertThat(reloaded.getStyles()).containsExactly("뉴");
    }

    @Test
    @DisplayName("delete 후 존재하지 않는다")
    void delete_thenAbsent() {
        Product saved = productRepository.save(newProduct("tmp", Set.of("X"), Set.of("Y")));

        productRepository.delete(saved);

        assertThat(productRepository.findById(saved.getId())).isEmpty();
        assertThat(productRepository.existsById(saved.getId())).isFalse();
    }
}
