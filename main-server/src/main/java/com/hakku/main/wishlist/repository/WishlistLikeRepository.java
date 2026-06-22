package com.hakku.main.wishlist.repository;

import com.hakku.main.wishlist.domain.WishlistLike;
import java.time.Instant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 찜 좋아요 MyBatis 매퍼. SQL 은 {@code mapper/wishlist/WishlistLikeMapper.xml} 에 정의한다.
 */
@Mapper
public interface WishlistLikeRepository {

    boolean existsByWishlistIdAndUserId(@Param("wishlistId") Long wishlistId,
                                        @Param("userId") Long userId);

    void deleteByWishlistIdAndUserId(@Param("wishlistId") Long wishlistId,
                                     @Param("userId") Long userId);

    long countByWishlistId(@Param("wishlistId") Long wishlistId);

    void insert(WishlistLike wishlistLike);

    /** 찜 좋아요를 저장한다. createdAt 만 있는 엔티티라 항상 신규 insert 다(JPA save() 의미 보존). */
    default WishlistLike save(WishlistLike wishlistLike) {
        wishlistLike.assignCreationTime(Instant.now());
        insert(wishlistLike);
        return wishlistLike;
    }
}
