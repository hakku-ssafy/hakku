package com.hakku.main.wishlist;

/** 찜 좋아요 토글 응답 DTO. */
public record WishlistLikeResponse(boolean liked, long likeCount) {
}
