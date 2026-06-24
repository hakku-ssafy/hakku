package com.hakku.main.auth.exception;

/**
 * 리프레시 토큰이 없거나(쿠키 부재) 위조·만료·형식오류로 신뢰할 수 없는 경우.
 *
 * <p>구체 사유를 노출하지 않도록 모든 실패를 동일한 예외/메시지로 401 처리한다.
 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("유효하지 않은 리프레시 토큰입니다.");
    }
}
