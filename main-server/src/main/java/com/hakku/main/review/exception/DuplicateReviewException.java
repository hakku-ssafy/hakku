package com.hakku.main.review.exception;

/**
 * 한 회원이 같은 상품에 이미 리뷰를 작성했는데 다시 작성하려 할 때 발생. HTTP 409 로 매핑된다.
 */
public class DuplicateReviewException extends RuntimeException {

    public DuplicateReviewException(Long productId) {
        super("이미 리뷰를 작성한 상품입니다: productId=" + productId);
    }
}
