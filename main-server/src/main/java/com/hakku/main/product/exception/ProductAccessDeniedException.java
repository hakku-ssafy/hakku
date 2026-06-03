package com.hakku.main.product.exception;

/**
 * 판매자 권한이 없거나(상품 등록) 등록한 판매자가 아닌 회원이 상품을 수정/삭제하려 할 때 발생.
 * HTTP 403 으로 매핑된다.
 */
public class ProductAccessDeniedException extends RuntimeException {

    public ProductAccessDeniedException(String message) {
        super(message);
    }
}
