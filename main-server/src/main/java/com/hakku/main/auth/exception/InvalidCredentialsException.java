package com.hakku.main.auth.exception;

/**
 * 로그인 자격 증명이 올바르지 않은 경우(없는 이메일 또는 비밀번호 불일치).
 *
 * <p>이메일 존재 여부를 노출하지 않도록 두 경우 모두 동일한 예외/메시지를 사용한다.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
}
