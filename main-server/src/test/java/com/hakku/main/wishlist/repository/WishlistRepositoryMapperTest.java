package com.hakku.main.wishlist.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.hakku.main.product.domain.Product;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.user.domain.Role;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import com.hakku.main.wishlist.domain.Wishlist;
import com.hakku.main.wishlist.domain.WishlistLike;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * WishlistRepository / WishlistLikeRepository(MyBatis 매퍼) 통합 테스트.
 * H2 스키마는 Flyway 마이그레이션(실제 운영 스키마)으로 제공되며 FK 제약을 강제한다.
 * 따라서 찜/찜좋아요를 넣기 전에 참조 부모 행(users, products, wishlists)을 먼저 생성하고 생성된 실제 id 를 사용한다.
 */
@SpringBootTest
@Transactional
class WishlistRepositoryMapperTest {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private WishlistLikeRepository wishlistLikeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private Long productId;
    private Long productId2;
    private Long userId;
    private Long likerId;

    @BeforeEach
    void setUp() {
        User seller = userRepository.save(new User("wishlist-seller@test.com", "판매자", Role.SELLER));
        userId = userRepository.save(new User("wishlist-owner@test.com", "찜주인", Role.NORMAL)).getId();
        likerId = userRepository.save(new User("wishlist-liker@test.com", "찜좋아요누른이", Role.NORMAL)).getId();
        productId = productRepository.save(new Product(
                seller.getId(), "상품1", "설명", 10000L,
                "TOP", "WINTER", "SUMMER", null, null, null, null)).getId();
        productId2 = productRepository.save(new Product(
                seller.getId(), "상품2", "설명", 20000L,
                "TOP", "SPRING", "AUTUMN", null, null, null, null)).getId();
    }

    @Test
    @DisplayName("찜 save 후 존재/카운트/단건조회가 반영된다")
    void wishlist_saveThenQuery() {
        Wishlist saved = wishlistRepository.save(new Wishlist(productId, userId));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(wishlistRepository.existsByProductIdAndUserId(productId, userId)).isTrue();
        assertThat(wishlistRepository.countByProductId(productId)).isEqualTo(1L);
        assertThat(wishlistRepository.findById(saved.getId())).isPresent();
        assertThat(wishlistRepository.findById(999L)).isEmpty();
    }

    @Test
    @DisplayName("회원의 찜 목록을 id 내림차순으로 조회한다")
    void wishlist_listByUserDesc() {
        wishlistRepository.save(new Wishlist(productId, userId));
        wishlistRepository.save(new Wishlist(productId2, userId));

        List<Wishlist> list = wishlistRepository.findByUserIdOrderByIdDesc(userId);

        assertThat(list).extracting(Wishlist::getProductId).containsExactly(productId2, productId);
    }

    @Test
    @DisplayName("찜 삭제 후 존재하지 않는다")
    void wishlist_delete() {
        wishlistRepository.save(new Wishlist(productId, userId));

        wishlistRepository.deleteByProductIdAndUserId(productId, userId);

        assertThat(wishlistRepository.existsByProductIdAndUserId(productId, userId)).isFalse();
    }

    @Test
    @DisplayName("찜 좋아요 save/존재/카운트/삭제 라운드트립")
    void wishlistLike_roundTrip() {
        Wishlist wishlist = wishlistRepository.save(new Wishlist(productId, userId));
        Long wishlistId = wishlist.getId();

        WishlistLike like = wishlistLikeRepository.save(new WishlistLike(wishlistId, likerId));
        assertThat(like.getId()).isNotNull();
        assertThat(like.getCreatedAt()).isNotNull();
        assertThat(wishlistLikeRepository.existsByWishlistIdAndUserId(wishlistId, likerId)).isTrue();
        assertThat(wishlistLikeRepository.countByWishlistId(wishlistId)).isEqualTo(1L);
    }

    @Test
    @DisplayName("찜 좋아요 삭제 후 존재하지 않는다")
    void wishlistLike_delete() {
        Wishlist wishlist = wishlistRepository.save(new Wishlist(productId, userId));
        Long wishlistId = wishlist.getId();
        wishlistLikeRepository.save(new WishlistLike(wishlistId, likerId));

        wishlistLikeRepository.deleteByWishlistIdAndUserId(wishlistId, likerId);

        assertThat(wishlistLikeRepository.countByWishlistId(wishlistId)).isEqualTo(0L);
        assertThat(wishlistLikeRepository.existsByWishlistIdAndUserId(wishlistId, likerId)).isFalse();
    }
}
