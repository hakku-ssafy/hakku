package com.hakku.main.wishlist.repository;

import com.hakku.main.wishlist.domain.WishlistLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistLikeRepository extends JpaRepository<WishlistLike, Long> {

    boolean existsByWishlistIdAndUserId(Long wishlistId, Long userId);

    void deleteByWishlistIdAndUserId(Long wishlistId, Long userId);

    long countByWishlistId(Long wishlistId);
}
