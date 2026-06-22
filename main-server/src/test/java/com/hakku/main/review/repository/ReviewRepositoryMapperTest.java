package com.hakku.main.review.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.hakku.main.product.domain.Product;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.review.domain.Review;
import com.hakku.main.review.repository.ReviewRepository.ProductRatingAverage;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * ReviewRepository(MyBatis 매퍼) 통합 테스트. 평점 평균 집계 쿼리(추천 피처)도 검증한다.
 * H2 스키마는 Flyway 마이그레이션(실제 운영 스키마)으로 제공되며 FK 제약을 강제한다.
 * 따라서 리뷰를 넣기 전에 참조 부모 행(users, products)을 먼저 생성하고 생성된 실제 id 를 사용한다.
 */
@SpringBootTest
@Transactional
class ReviewRepositoryMapperTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private Long productId;
    private Long productId2;
    private Long authorId;
    private Long authorId2;
    private Long authorId3;

    @BeforeEach
    void setUp() {
        User seller = userRepository.save(new User("review-seller@test.com", "판매자", Role.SELLER));
        authorId = userRepository.save(new User("review-author1@test.com", "리뷰어1", Role.NORMAL)).getId();
        authorId2 = userRepository.save(new User("review-author2@test.com", "리뷰어2", Role.NORMAL)).getId();
        authorId3 = userRepository.save(new User("review-author3@test.com", "리뷰어3", Role.NORMAL)).getId();
        productId = productRepository.save(new Product(
                seller.getId(), "상품1", "설명", 10000L,
                "TOP", "WINTER", "SUMMER", null, null, null, null)).getId();
        productId2 = productRepository.save(new Product(
                seller.getId(), "상품2", "설명", 20000L,
                "TOP", "SPRING", "AUTUMN", null, null, null, null)).getId();
    }

    @Test
    @DisplayName("save(신규) 후 단건/존재/목록 조회가 반영되고 타임스탬프가 채워진다")
    void insert_thenQuery() {
        Review saved = reviewRepository.save(new Review(productId, authorId, 4, "좋아요"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(reviewRepository.findById(saved.getId())).isPresent();
        assertThat(reviewRepository.existsByProductIdAndAuthorId(productId, authorId)).isTrue();
        assertThat(reviewRepository.findByProductIdOrderByIdDesc(productId)).hasSize(1);
        assertThat(reviewRepository.findByAuthorIdOrderByIdDesc(authorId)).hasSize(1);
    }

    @Test
    @DisplayName("save(갱신) 시 평점/내용이 변경된다")
    void update_changesRatingAndContent() {
        Review saved = reviewRepository.save(new Review(productId, authorId, 3, "보통"));

        saved.update(5, "최고");
        reviewRepository.save(saved);

        Optional<Review> reloaded = reviewRepository.findById(saved.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getRating()).isEqualTo(5);
        assertThat(reloaded.get().getContent()).isEqualTo("최고");
    }

    @Test
    @DisplayName("상품별 평점 평균을 집계한다")
    void findAverageRatingByProduct() {
        reviewRepository.save(new Review(productId, authorId, 4, "a"));
        reviewRepository.save(new Review(productId, authorId2, 2, "b"));
        reviewRepository.save(new Review(productId2, authorId3, 5, "c"));

        Map<Long, Double> averages = reviewRepository.findAverageRatingByProduct().stream()
                .collect(Collectors.toMap(
                        ProductRatingAverage::getProductId,
                        ProductRatingAverage::getAverage));

        assertThat(averages.get(productId)).isCloseTo(3.0, within(1e-9));
        assertThat(averages.get(productId2)).isCloseTo(5.0, within(1e-9));
    }

    @Test
    @DisplayName("delete 후 존재하지 않는다")
    void delete_thenAbsent() {
        Review saved = reviewRepository.save(new Review(productId, authorId, 4, "tmp"));

        reviewRepository.delete(saved);

        assertThat(reviewRepository.findById(saved.getId())).isEmpty();
        assertThat(reviewRepository.findByProductIdOrderByIdDesc(productId)).isEmpty();
    }
}
