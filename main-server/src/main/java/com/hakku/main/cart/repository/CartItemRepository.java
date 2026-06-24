package com.hakku.main.cart.repository;

import com.hakku.main.cart.domain.CartItem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 장바구니 항목 MyBatis 매퍼. SQL 은 {@code mapper/cart/CartItemMapper.xml} 에 정의한다.
 */
@Mapper
public interface CartItemRepository {

    CartItem selectByUserIdAndProductId(@Param("userId") Long userId,
                                        @Param("productId") Long productId);

    List<CartItem> findByUserId(@Param("userId") Long userId);

    /** 소유권 검증과 조회를 한 번에: 본인 항목만 반환한다. */
    CartItem selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    void insert(CartItem cartItem);

    void update(CartItem cartItem);

    void deleteById(@Param("id") Long id);

    /** 회원의 장바구니를 비운다(주문 결제 완료 시 호출). */
    void deleteByUserId(@Param("userId") Long userId);

    /** 회원-상품으로 단건 조회한다. 서비스가 .map()/.orElseGet() 로 쓰므로 Optional 로 감싼다. */
    default Optional<CartItem> findByUserIdAndProductId(Long userId, Long productId) {
        return Optional.ofNullable(selectByUserIdAndProductId(userId, productId));
    }

    /** 본인 항목만 단건 조회한다. 서비스가 .orElseThrow() 로 쓰므로 Optional 로 감싼다. */
    default Optional<CartItem> findByIdAndUserId(Long id, Long userId) {
        return Optional.ofNullable(selectByIdAndUserId(id, userId));
    }

    /** 엔티티를 삭제한다(JPA delete(entity) 의미 보존). */
    default void delete(CartItem cartItem) {
        deleteById(cartItem.getId());
    }

    /** 장바구니 항목을 저장한다. id 가 없으면 신규(insert), 있으면 갱신(update). JPA save() 의미 보존. */
    default CartItem save(CartItem cartItem) {
        if (cartItem.getId() == null) {
            cartItem.assignCreationTime(Instant.now());
            insert(cartItem);
        } else {
            cartItem.assignUpdateTime(Instant.now());
            update(cartItem);
        }
        return cartItem;
    }
}
