package com.hakku.main.community.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hakku.main.community.RelatedProductSummary;
import com.hakku.main.community.domain.Post;
import com.hakku.main.community.domain.PostBoard;
import com.hakku.main.product.domain.Product;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostProductRepository(연관 상품 링크 MyBatis 매퍼) 통합 테스트.
 * 실제 운영 스키마(Flyway)로 H2 를 구성하므로 FK 제약이 강제된다.
 */
@SpringBootTest
@Transactional
class PostProductRepositoryMapperTest {

    @Autowired
    private PostProductRepository postProductRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private Long postId;
    private Long productA;
    private Long productB;

    @BeforeEach
    void setUp() {
        Long sellerId = userRepository.save(new User("seller@test.com", "셀러", Role.NORMAL)).getId();
        Long authorId = userRepository.save(new User("author@test.com", "글쓴이", Role.NORMAL)).getId();
        productA = productRepository.save(new Product(sellerId, "키링", "다꾸 키링", 4900L, "키링",
                "WINTER", null, Set.of(), Set.of("미니멀"), "https://img/a.png", null)).getId();
        productB = productRepository.save(new Product(sellerId, "스티커", "꾸미기 스티커", 2500L, "꾸미기 스티커",
                "SPRING", null, Set.of(), Set.of("러블리"), "https://img/b.png", null)).getId();
        postId = postRepository.save(
                new Post(authorId, "내 학생증", "꾸며봤어요", PostBoard.STUDENT_ID, "https://img/card.png")).getId();
    }

    @Test
    @DisplayName("insert 후 findRelatedProductsByPostId 는 position 순서로 상품 요약을 반환한다")
    void insert_thenFindOrdered() {
        postProductRepository.insert(postId, productB, 0);
        postProductRepository.insert(postId, productA, 1);

        List<RelatedProductSummary> related = postProductRepository.findRelatedProductsByPostId(postId);

        assertThat(related).extracting(RelatedProductSummary::id).containsExactly(productB, productA);
        assertThat(related.get(0).name()).isEqualTo("스티커");
        assertThat(related.get(0).imageUrl()).isEqualTo("https://img/b.png");
        assertThat(related.get(0).price()).isEqualTo(2500L);
    }

    @Test
    @DisplayName("insert 는 존재하지 않는 상품 id 면 아무 것도 저장하지 않는다(EXISTS 가드)")
    void insert_missingProduct_noop() {
        postProductRepository.insert(postId, 999_999L, 0);

        assertThat(postProductRepository.findRelatedProductsByPostId(postId)).isEmpty();
    }
}
