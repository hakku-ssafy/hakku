package com.hakku.main.review.exception;

/**
 * 요청한 리뷰가 존재하지 않을 때 발생. HTTP 404 로 매핑된다.
 */
public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException(Long reviewId) {
        super("리뷰를 찾을 수 없습니다: id=" + reviewId);
    }
}
