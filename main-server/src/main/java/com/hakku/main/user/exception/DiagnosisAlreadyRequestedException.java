package com.hakku.main.user.exception;

/** 이미 진단 요청이 진행 중이거나 완료된 경우. */
public class DiagnosisAlreadyRequestedException extends RuntimeException {
    public DiagnosisAlreadyRequestedException(Long userId) {
        super("이미 진단 요청이 진행 중이거나 완료되었습니다: userId=" + userId);
    }
}
