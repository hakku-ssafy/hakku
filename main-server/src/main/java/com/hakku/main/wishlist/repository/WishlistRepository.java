package com.hakku.main.wishlist.repository;

import com.hakku.main.wishlist.domain.Wishlist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByProductIdAndUserId(Long productId, Long userId);

    void deleteByProductIdAndUserId(Long productId, Long userId);

    long countByProductId(Long productId);

    /** 한 회원의 찜 목록을 최신순(id 내림차순)으로 조회한다. */
    List<Wishlist> findByUserIdOrderByIdDesc(Long userId);
}
