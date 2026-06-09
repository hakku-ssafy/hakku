package com.hakku.main.wishlist;

import com.hakku.main.notification.NotificationEvent;
import com.hakku.main.notification.NotificationMessageFormatter;
import com.hakku.main.notification.NotificationProducer;
import com.hakku.main.notification.domain.NotificationType;
import com.hakku.main.product.domain.Product;
import com.hakku.main.product.exception.ProductNotFoundException;
import com.hakku.main.product.repository.ProductRepository;
import com.hakku.main.user.domain.User;
import com.hakku.main.user.repository.UserRepository;
import com.hakku.main.wishlist.domain.Wishlist;
import com.hakku.main.wishlist.domain.WishlistLike;
import com.hakku.main.wishlist.exception.WishlistNotFoundException;
import com.hakku.main.wishlist.repository.WishlistLikeRepository;
import com.hakku.main.wishlist.repository.WishlistRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 찜과 찜 좋아요 (요구사항 §2). 찜은 토글 방식이며, 다른 회원이 내 찜에 좋아요를 누르면 알림이 온다.
 */
@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistLikeRepository wishlistLikeRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationProducer notificationProducer;

    public WishlistService(WishlistRepository wishlistRepository,
                           WishlistLikeRepository wishlistLikeRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository,
                           NotificationProducer notificationProducer) {
        this.wishlistRepository = wishlistRepository;
        this.wishlistLikeRepository = wishlistLikeRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.notificationProducer = notificationProducer;
    }

    /** 상품 찜을 토글한다. 누르지 않았으면 찜, 이미 찜했으면 해제. */
    @Transactional
    public WishlistToggleResponse toggleWishlist(Long productId, Long userId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        boolean wishlisted;
        if (wishlistRepository.existsByProductIdAndUserId(productId, userId)) {
            wishlistRepository.deleteByProductIdAndUserId(productId, userId);
            wishlisted = false;
        } else {
            wishlistRepository.save(new Wishlist(productId, userId));
            wishlisted = true;
        }
        return new WishlistToggleResponse(wishlisted, wishlistRepository.countByProductId(productId));
    }

    /** 특정 상품의 찜 상태를 조회한다(상품 상세 화면용). 비로그인(viewerId=null)이면 wishlisted=false. */
    @Transactional(readOnly = true)
    public WishlistToggleResponse getStatus(Long productId, Long userId) {
        boolean wishlisted = userId != null
                && wishlistRepository.existsByProductIdAndUserId(productId, userId);
        return new WishlistToggleResponse(wishlisted, wishlistRepository.countByProductId(productId));
    }

    /** 한 회원의 찜 목록을 조회한다. viewerId 기준으로 좋아요 여부(likedByMe)를 채운다. */
    @Transactional(readOnly = true)
    public List<WishlistResponse> listByUser(Long ownerId, Long viewerId) {
        String ownerNickname = userRepository.findById(ownerId)
                .map(User::getNickname)
                .orElse("알 수 없음");
        return wishlistRepository.findByUserIdOrderByIdDesc(ownerId).stream()
                .map(wishlist -> toResponse(wishlist, ownerId, ownerNickname, viewerId))
                .toList();
    }

    /** 찜에 대한 좋아요를 토글한다. 새로 좋아요를 누르면 찜 주인에게 알림이 간다(본인 제외). */
    @Transactional
    public WishlistLikeResponse toggleLike(Long wishlistId, Long userId) {
        Wishlist wishlist = wishlistRepository.findById(wishlistId)
                .orElseThrow(() -> new WishlistNotFoundException(wishlistId));
        boolean liked;
        if (wishlistLikeRepository.existsByWishlistIdAndUserId(wishlistId, userId)) {
            wishlistLikeRepository.deleteByWishlistIdAndUserId(wishlistId, userId);
            liked = false;
        } else {
            wishlistLikeRepository.save(new WishlistLike(wishlistId, userId));
            liked = true;
            if (!wishlist.getUserId().equals(userId)) {
                notifyOwner(wishlist, userId);
            }
        }
        return new WishlistLikeResponse(liked, wishlistLikeRepository.countByWishlistId(wishlistId));
    }

    private void notifyOwner(Wishlist wishlist, Long actorId) {
        String actorNickname = userRepository.findById(actorId)
                .map(User::getNickname)
                .orElse("누군가");
        String productName = productRepository.findById(wishlist.getProductId())
                .map(Product::getName)
                .orElse("상품");
        notificationProducer.publish(new NotificationEvent(
                NotificationType.WISHLIST_LIKE,
                wishlist.getUserId(),
                actorId,
                actorNickname,
                null,
                null,
                wishlist.getProductId(),
                NotificationMessageFormatter.wishlistLike(actorNickname, productName),
                Instant.now().toEpochMilli()));
    }

    private WishlistResponse toResponse(Wishlist wishlist, Long ownerId, String ownerNickname, Long viewerId) {
        Product product = productRepository.findById(wishlist.getProductId()).orElse(null);
        long likeCount = wishlistLikeRepository.countByWishlistId(wishlist.getId());
        boolean likedByMe = viewerId != null
                && wishlistLikeRepository.existsByWishlistIdAndUserId(wishlist.getId(), viewerId);
        return new WishlistResponse(
                wishlist.getId(),
                wishlist.getProductId(),
                product != null ? product.getName() : "삭제된 상품",
                product != null ? product.getImageUrl() : null,
                product != null ? product.getPrice() : 0L,
                ownerId,
                ownerNickname,
                likeCount,
                likedByMe,
                wishlist.getCreatedAt());
    }
}
