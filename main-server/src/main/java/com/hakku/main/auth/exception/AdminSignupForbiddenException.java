package com.hakku.main.auth.exception;

/**
 * 공개 회원가입(/api/auth/signup)으로 ADMIN 역할을 자가 부여하려는 시도.
 * 관리자는 시드/프로비저닝 경로로만 생성하므로 signup 에서는 거부한다(권한 상승 차단).
 */
public class AdminSignupForbiddenException extends RuntimeException {
    public AdminSignupForbiddenException() {
        super("관리자 권한으로는 회원가입할 수 없습니다.");
    }
}
