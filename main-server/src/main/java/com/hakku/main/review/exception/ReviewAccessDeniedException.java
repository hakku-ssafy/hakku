package com.hakku.main.review.exception;

/**
 * 작성자가 아닌 회원이 리뷰를 수정/삭제하려 할 때 발생. HTTP 403 으로 매핑된다.
 */
public class ReviewAccessDeniedException extends RuntimeException {

    public ReviewAccessDeniedException(Long reviewId) {
        super("리뷰에 대한 권한이 없습니다: id=" + reviewId);
    }
}
