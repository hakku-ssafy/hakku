package com.hakku.main.review.repository;

import com.hakku.main.review.domain.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 상품의 리뷰를 최신순(id 내림차순)으로 조회한다. */
    List<Review> findByProductIdOrderByIdDesc(Long productId);

    boolean existsByProductIdAndAuthorId(Long productId, Long authorId);
}
