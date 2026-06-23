package com.hakku.main.review;

import com.hakku.main.product.domain.Product;
import com.hakku.main.product.exception.ProductNotFoundException;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.review.domain.Review;
import com.hakku.main.review.exception.DuplicateReviewException;
import com.hakku.main.review.exception.ReviewAccessDeniedException;
import com.hakku.main.review.exception.ReviewNotFoundException;
import com.hakku.main.review.repository.ReviewRepository;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 리뷰 CRUD (PRD §3.3). 회원-상품 당 한 번만 작성 가능하고, 수정/삭제는 작성자 본인만 가능하다.
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewResponse create(Long productId, Long authorId, int rating, String content) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        if (reviewRepository.existsByProductIdAndAuthorId(productId, authorId)) {
            throw new DuplicateReviewException(productId);
        }
        Review saved = reviewRepository.save(new Review(productId, authorId, rating, content));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listByProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByIdDesc(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** 한 회원이 작성한 리뷰를 최신순으로 조회한다(마이페이지/프로필용). */
    @Transactional(readOnly = true)
    public List<ReviewResponse> listByAuthor(Long authorId) {
        return reviewRepository.findByAuthorIdOrderByIdDesc(authorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReviewResponse update(Long reviewId, Long requesterId, int rating, String content) {
        Review review = findOrThrow(reviewId);
        requireAuthor(review, requesterId);
        review.update(rating, content);
        reviewRepository.save(review);
        return toResponse(review);
    }

    @Transactional
    public void delete(Long reviewId, Long requesterId) {
        Review review = findOrThrow(reviewId);
        requireAuthor(review, requesterId);
        reviewRepository.delete(review);
    }

    private ReviewResponse toResponse(Review review) {
        String authorNickname = userRepository.findById(review.getAuthorId())
                .map(User::getNickname)
                .orElse("알 수 없음");
        String productName = productRepository.findById(review.getProductId())
                .map(Product::getName)
                .orElse("삭제된 상품");
        return ReviewResponse.from(review, authorNickname, productName);
    }

    private Review findOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(reviewId));
    }

    private void requireAuthor(Review review, Long requesterId) {
        if (!review.isAuthor(requesterId)) {
            throw new ReviewAccessDeniedException(review.getId());
        }
    }
}
