package com.hakku.main.cart.repository;

import com.hakku.main.cart.domain.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId);

    List<CartItem> findByUserId(Long userId);

    /** 소유권 검증과 조회를 한 번에: 본인 항목만 반환한다. */
    Optional<CartItem> findByIdAndUserId(Long id, Long userId);
}
