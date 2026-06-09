package com.hakku.main.wishlist;

/** 상품 찜 토글/상태 응답 DTO. */
public record WishlistToggleResponse(boolean wishlisted, long wishlistCount) {
}
