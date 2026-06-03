package com.hakku.main.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

import com.hakku.main.personalcolor.domain.PersonalColorType;
import com.hakku.main.product.domain.Product;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.review.repository.ReviewRepository;
import com.hakku.main.review.repository.ReviewRepository.ProductRatingAverage;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.exception.UserNotFoundException;
import com.hakku.main.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 추천 점수기(§3.5)를 회원 프로필 + 상품 피처에 적용해 정렬된 추천 목록을 만드는 서비스의 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    private static final Long USER = 1L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(userRepository, productRepository, reviewRepository);
    }

    @Test
    @DisplayName("recommendFor: 색상·스타일이 맞을수록 높은 점수로 내림차순 정렬한다")
    void recommendFor_ordersByScoreDescending() {
        // 회원: 여름 타입(계절=SUMMER), 선호 스타일 '미니멀'
        when(userRepository.findById(USER))
                .thenReturn(Optional.of(userWith(PersonalColorType.LIGHT_SUMMER, Set.of("미니멀"))));
        // 상품들은 리포지토리가 id 내림차순으로 반환한다(최신 우선).
        Product match = product(30L, "여름미니멀", "SUMMER", null, Set.of("미니멀"));
        Product sub = product(20L, "여름서브", null, "SUMMER", Set.of());
        Product none = product(10L, "겨울무관", "WINTER", null, Set.of());
        when(productRepository.findAllByOrderByIdDesc()).thenReturn(List.of(match, sub, none));
        when(reviewRepository.findAverageRatingByProduct()).thenReturn(List.of());

        List<RecommendationResponse> result = service.recommendFor(USER);

        assertThat(result).extracting(r -> r.product().name())
                .containsExactly("여름미니멀", "여름서브", "겨울무관");
        // 키컬러(3.0) + 스타일 전부커버(2.0) = 5.0
        assertThat(result.get(0).score()).isEqualTo(5.0);
        assertThat(result.get(0).breakdown().personalColor()).isEqualTo(3.0);
        assertThat(result.get(0).breakdown().style()).isEqualTo(2.0);
        // 서브컬러(1.5)
        assertThat(result.get(1).score()).isEqualTo(1.5);
        // 매칭 없음 → 0.0
        assertThat(result.get(2).score()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("recommendFor: 상품 리뷰 평점 평균이 review 점수로 반영된다")
    void recommendFor_includesReviewAverage() {
        when(userRepository.findById(USER))
                .thenReturn(Optional.of(userWith(null, Set.of()))); // 색상/스타일 신호 없음
        Product p = product(10L, "리뷰상품", null, null, Set.of());
        when(productRepository.findAllByOrderByIdDesc()).thenReturn(List.of(p));
        when(reviewRepository.findAverageRatingByProduct())
                .thenReturn(List.of(ratingAverage(10L, 4.0)));

        List<RecommendationResponse> result = service.recommendFor(USER);

        // 평점 4.0 → 정규화 4.0/5.0 * 1.0 = 0.8
        assertThat(result.get(0).breakdown().review()).isCloseTo(0.8, within(1e-9));
        assertThat(result.get(0).score()).isCloseTo(0.8, within(1e-9));
    }

    @Test
    @DisplayName("recommendFor: 없는 회원이면 UserNotFoundException")
    void recommendFor_missingUser_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recommendFor(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    private User userWith(PersonalColorType color, Set<String> preferredStyles) {
        User user = new User("u@hakku.dev", "회원", Role.NORMAL);
        if (color != null) {
            user.assignPersonalColor(color);
        }
        user.updateProfile("회원", null, preferredStyles);
        return user;
    }

    private Product product(long id, String name, String keyColor, String subColor, Set<String> styles) {
        Product product = new Product(2L, name, "설명", 10000L, keyColor, subColor, styles, null);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private ProductRatingAverage ratingAverage(Long productId, Double average) {
        return new ProductRatingAverage() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public Double getAverage() {
                return average;
            }
        };
    }
}
