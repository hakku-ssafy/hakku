package com.hakku.main.product.exception;

/**
 * 요청한 상품이 존재하지 않을 때 발생. HTTP 404 로 매핑된다.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("상품을 찾을 수 없습니다: id=" + productId);
    }
}
