package com.hakku.main.review;

import com.hakku.main.product.exception.ProductNotFoundException;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.review.domain.Review;
import com.hakku.main.review.exception.DuplicateReviewException;
import com.hakku.main.review.exception.ReviewAccessDeniedException;
import com.hakku.main.review.exception.ReviewNotFoundException;
import com.hakku.main.review.repository.ReviewRepository;
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

    public ReviewService(ReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
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
        return ReviewResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listByProduct(Long productId) {
        return reviewRepository.findByProductIdOrderByIdDesc(productId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    @Transactional
    public ReviewResponse update(Long reviewId, Long requesterId, int rating, String content) {
        Review review = findOrThrow(reviewId);
        requireAuthor(review, requesterId);
        review.update(rating, content);
        return ReviewResponse.from(review);
    }

    @Transactional
    public void delete(Long reviewId, Long requesterId) {
        Review review = findOrThrow(reviewId);
        requireAuthor(review, requesterId);
        reviewRepository.delete(review);
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
