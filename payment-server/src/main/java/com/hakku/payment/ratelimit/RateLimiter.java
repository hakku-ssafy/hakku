package com.hakku.payment.ratelimit;

/**
 * 키(인증 사용자 id 또는 클라이언트 IP) 단위 레이트리밋(H-4).
 *
 * <p>현재 구현은 인스턴스 로컬 토큰 버킷이다 — 다중 인스턴스 전역 한도가 필요하면 같은 인터페이스 뒤에서
 * Redis 백엔드(예: Bucket4j + Lettuce) 구현으로 교체한다(주입부 변경 없음).
 */
public interface RateLimiter {

    /** 주어진 키에 토큰 1개 소비를 시도한다. 한도 내면 {@code true}, 초과면 {@code false}. */
    boolean tryAcquire(String key);
}
