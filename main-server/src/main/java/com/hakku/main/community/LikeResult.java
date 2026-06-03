package com.hakku.main.community;

/**
 * 좋아요 토글 결과. liked=현재 회원의 좋아요 여부, likeCount=게시글의 총 좋아요 수.
 */
public record LikeResult(boolean liked, long likeCount) {
}
