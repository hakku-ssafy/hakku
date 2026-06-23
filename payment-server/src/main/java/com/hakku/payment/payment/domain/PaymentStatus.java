package com.hakku.payment.payment.domain;

/**
 * 결제 상태. {@link #PENDING} 만 비종료 상태이며, 나머지는 종료 상태다.
 *
 * <ul>
 *   <li>{@link #PENDING}   — 결제 요청됨, PG 응답 대기</li>
 *   <li>{@link #APPROVED}  — PG 승인 완료(종료)</li>
 *   <li>{@link #FAILED}    — PG 승인 실패(종료)</li>
 *   <li>{@link #CANCELLED} — 승인 전 취소/중단(종료)</li>
 * </ul>
 */
public enum PaymentStatus {
    PENDING,
    APPROVED,
    FAILED,
    CANCELLED
}
