package com.hakku.main.wishlist.repository;

import com.hakku.main.wishlist.domain.Wishlist;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 상품 찜 MyBatis 매퍼. SQL 은 {@code mapper/wishlist/WishlistMapper.xml} 에 정의한다.
 */
@Mapper
public interface WishlistRepository {

    boolean existsByProductIdAndUserId(@Param("productId") Long productId,
                                       @Param("userId") Long userId);

    void deleteByProductIdAndUserId(@Param("productId") Long productId,
                                    @Param("userId") Long userId);

    long countByProductId(@Param("productId") Long productId);

    /** 한 회원의 찜 목록을 최신순(id 내림차순)으로 조회한다. */
    List<Wishlist> findByUserIdOrderByIdDesc(@Param("userId") Long userId);

    Wishlist selectById(@Param("id") Long id);

    void insert(Wishlist wishlist);

    /** id 로 단건 조회한다. 서비스가 .orElseThrow() 로 쓰므로 Optional 로 감싼다. */
    default Optional<Wishlist> findById(Long id) {
        return Optional.ofNullable(selectById(id));
    }

    /** 찜을 저장한다. createdAt 만 있는 엔티티라 항상 신규 insert 다(JPA save() 의미 보존). */
    default Wishlist save(Wishlist wishlist) {
        wishlist.assignCreationTime(Instant.now());
        insert(wishlist);
        return wishlist;
    }
}
