package com.hakku.payment.payment.exception;

/**
 * 동일 멱등 키로 이전과 다른 내용(금액/참조/회원)의 결제 요청이 들어온 경우.
 * 키 재사용 또는 클라이언트의 잘못된 키 생성을 호출자에게 알린다(409 성격).
 */
public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("동일 멱등 키로 다른 결제 요청이 접수되었습니다: " + idempotencyKey);
    }
}
