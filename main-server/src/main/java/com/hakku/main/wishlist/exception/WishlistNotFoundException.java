package com.hakku.main.wishlist.exception;

/** 존재하지 않는 찜을 참조할 때. */
public class WishlistNotFoundException extends RuntimeException {

    public WishlistNotFoundException(Long wishlistId) {
        super("찜을 찾을 수 없습니다: id=" + wishlistId);
    }
}
