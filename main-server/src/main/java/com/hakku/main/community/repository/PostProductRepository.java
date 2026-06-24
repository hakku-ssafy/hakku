package com.hakku.main.community.repository;

import com.hakku.main.community.RelatedProductSummary;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 게시글-연관 상품 링크 MyBatis 매퍼. SQL 은 {@code mapper/community/PostProductMapper.xml} 에 정의한다.
 */
@Mapper
public interface PostProductRepository {

    /**
     * 게시글-상품 링크를 저장한다. 존재하지 않는 상품 id 는 무시한다(SELECT … FROM products WHERE id=? 가드).
     * FK 위반으로 트랜잭션이 깨지지 않도록 잘못된 상품 id 는 조용히 건너뛴다.
     */
    void insert(@Param("postId") Long postId,
                @Param("productId") Long productId,
                @Param("position") int position);

    /** 게시글의 연관 상품을 등록 순서(sort_order)대로 요약 조회한다. */
    List<RelatedProductSummary> findRelatedProductsByPostId(@Param("postId") Long postId);
}
