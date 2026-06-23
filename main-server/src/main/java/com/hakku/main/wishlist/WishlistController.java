package com.hakku.main.wishlist;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상품 찜 / 찜 좋아요 API (요구사항 §2). 모두 인증 필요.
 * 인증 주체(principal)는 JWT subject = 회원 id 문자열.
 */
@RestController
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping("/api/products/{productId}/wishlist")
    public WishlistToggleResponse toggle(@AuthenticationPrincipal String userId,
                                         @PathVariable Long productId) {
        return wishlistService.toggleWishlist(productId, Long.valueOf(userId));
    }

    @GetMapping("/api/products/{productId}/wishlist")
    public WishlistToggleResponse status(@AuthenticationPrincipal String userId,
                                         @PathVariable Long productId) {
        Long viewerId = userId != null ? Long.valueOf(userId) : null;
        return wishlistService.getStatus(productId, viewerId);
    }

    @GetMapping("/api/users/{id}/wishlist")
    public List<WishlistResponse> userWishlist(@AuthenticationPrincipal String userId,
                                               @PathVariable Long id) {
        return wishlistService.listByUser(id, Long.valueOf(userId));
    }

    @PostMapping("/api/wishlist/{wishlistId}/likes")
    public WishlistLikeResponse toggleLike(@AuthenticationPrincipal String userId,
                                           @PathVariable Long wishlistId) {
        return wishlistService.toggleLike(wishlistId, Long.valueOf(userId));
    }
}
