package com.hakku.main.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.main.product.exception.ProductNotFoundException;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.review.domain.Review;
import com.hakku.main.review.exception.DuplicateReviewException;
import com.hakku.main.review.exception.ReviewAccessDeniedException;
import com.hakku.main.review.exception.ReviewNotFoundException;
import com.hakku.main.review.repository.ReviewRepository;
import com.hakku.main.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Long PRODUCT = 10L;
    private static final Long AUTHOR = 1L;
    private static final Long OTHER = 2L;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, productRepository, userRepository);
    }

    @Test
    @DisplayName("create: 상품이 있고 첫 리뷰면 저장한다")
    void create_first_saves() {
        when(productRepository.existsById(PRODUCT)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndAuthorId(PRODUCT, AUTHOR)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewResponse response = reviewService.create(PRODUCT, AUTHOR, 5, "아주 좋아요");

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.content()).isEqualTo("아주 좋아요");
        assertThat(response.productId()).isEqualTo(PRODUCT);
    }

    @Test
    @DisplayName("create: 없는 상품이면 ProductNotFoundException")
    void create_missingProduct_throws() {
        when(productRepository.existsById(PRODUCT)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.create(PRODUCT, AUTHOR, 4, "x"))
                .isInstanceOf(ProductNotFoundException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("create: 같은 회원이 같은 상품에 두 번 리뷰하면 DuplicateReviewException")
    void create_duplicate_throws() {
        when(productRepository.existsById(PRODUCT)).thenReturn(true);
        when(reviewRepository.existsByProductIdAndAuthorId(PRODUCT, AUTHOR)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.create(PRODUCT, AUTHOR, 4, "또 작성"))
                .isInstanceOf(DuplicateReviewException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("listByProduct: 상품의 리뷰를 응답으로 변환한다")
    void listByProduct_returnsAll() {
        when(reviewRepository.findByProductIdOrderByIdDesc(PRODUCT))
                .thenReturn(List.of(review(), review()));

        List<ReviewResponse> all = reviewService.listByProduct(PRODUCT);

        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("update: 작성자 본인이면 평점/내용을 갱신한다")
    void update_byAuthor_updates() {
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review()));

        ReviewResponse response = reviewService.update(5L, AUTHOR, 3, "보통이에요");

        assertThat(response.rating()).isEqualTo(3);
        assertThat(response.content()).isEqualTo("보통이에요");
    }

    @Test
    @DisplayName("update: 작성자가 아니면 ReviewAccessDeniedException")
    void update_byOther_throws() {
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review()));

        assertThatThrownBy(() -> reviewService.update(5L, OTHER, 1, "x"))
                .isInstanceOf(ReviewAccessDeniedException.class);
    }

    @Test
    @DisplayName("update: 없는 리뷰면 ReviewNotFoundException")
    void update_missing_throws() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.update(99L, AUTHOR, 2, "x"))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("delete: 작성자가 아니면 거부하고 삭제하지 않는다")
    void delete_byOther_throws() {
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review()));

        assertThatThrownBy(() -> reviewService.delete(5L, OTHER))
                .isInstanceOf(ReviewAccessDeniedException.class);
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    @DisplayName("delete: 작성자 본인이면 삭제한다")
    void delete_byAuthor_deletes() {
        Review review = review();
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));

        reviewService.delete(5L, AUTHOR);

        verify(reviewRepository).delete(review);
    }

    private Review review() {
        return new Review(PRODUCT, AUTHOR, 5, "아주 좋아요");
    }
}
