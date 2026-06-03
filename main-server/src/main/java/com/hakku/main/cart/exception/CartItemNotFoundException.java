package com.hakku.main.cart.exception;

/**
 * 회원의 장바구니에 해당 항목이 없을 때 발생. 타인의 항목 접근도 동일하게 처리한다(존재 노출 방지).
 * HTTP 404 로 매핑된다.
 */
public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long cartItemId) {
        super("장바구니 항목을 찾을 수 없습니다: id=" + cartItemId);
    }
}
