package com.hakku.main.review.repository;

import com.hakku.main.review.domain.Review;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 리뷰 MyBatis 매퍼. SQL 은 {@code mapper/review/ReviewMapper.xml} 에 정의한다.
 */
@Mapper
public interface ReviewRepository {

    /** 상품의 리뷰를 최신순(id 내림차순)으로 조회한다. */
    List<Review> findByProductIdOrderByIdDesc(@Param("productId") Long productId);

    /** 한 회원이 작성한 리뷰를 최신순(id 내림차순)으로 조회한다(마이페이지/프로필용). */
    List<Review> findByAuthorIdOrderByIdDesc(@Param("authorId") Long authorId);

    boolean existsByProductIdAndAuthorId(@Param("productId") Long productId,
                                         @Param("authorId") Long authorId);

    /**
     * 상품별 평점 평균을 한 번의 쿼리로 집계한다(추천 엔진 §3.5의 reviewScore 피처). 상품마다
     * 리뷰를 따로 조회하는 N+1 을 피한다. 리뷰가 없는 상품은 결과에 포함되지 않는다.
     */
    List<ProductRatingAverage> findAverageRatingByProduct();

    Review selectById(@Param("id") Long id);

    void insert(Review review);

    void update(Review review);

    void deleteById(@Param("id") Long id);

    /** id 로 단건 조회한다. 서비스가 .orElseThrow() 로 쓰므로 Optional 로 감싼다. */
    default Optional<Review> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    /** 엔티티를 삭제한다(JPA delete(entity) 의미 보존). */
    default void delete(Review review) {
        deleteById(review.getId());
    }

    /** 리뷰를 저장한다. id 가 없으면 신규(insert), 있으면 갱신(update). JPA save() 의미 보존. */
    default Review save(Review review) {
        if (review.getId() == null) {
            review.assignCreationTime(Instant.now());
            insert(review);
        } else {
            review.assignUpdateTime(Instant.now());
            update(review);
        }
        return review;
    }

    /** {@link #findAverageRatingByProduct()} 의 행 사영(projection). 구현체는 {@link ProductRatingAverageRow}. */
    interface ProductRatingAverage {
        Long getProductId();

        Double getAverage();
    }
}
