package com.hakku.main.review.repository;

import com.hakku.main.review.domain.Review;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 상품의 리뷰를 최신순(id 내림차순)으로 조회한다. */
    List<Review> findByProductIdOrderByIdDesc(Long productId);

    boolean existsByProductIdAndAuthorId(Long productId, Long authorId);

    /**
     * 상품별 평점 평균을 한 번의 쿼리로 집계한다(추천 엔진 §3.5의 reviewScore 피처). 상품마다
     * 리뷰를 따로 조회하는 N+1 을 피한다. 리뷰가 없는 상품은 결과에 포함되지 않는다.
     */
    @Query("select r.productId as productId, avg(r.rating) as average from Review r group by r.productId")
    List<ProductRatingAverage> findAverageRatingByProduct();

    /** {@link #findAverageRatingByProduct()} 의 행 사영(projection). */
    interface ProductRatingAverage {
        Long getProductId();

        Double getAverage();
    }
}
