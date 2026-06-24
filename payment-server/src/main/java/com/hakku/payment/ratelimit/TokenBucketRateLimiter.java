package com.hakku.payment.ratelimit;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 인스턴스 로컬 토큰 버킷 레이트리밋(H-4). 키별로 {@code capacity} 만큼의 토큰을 두고 초당
 * {@code refill-per-second} 만큼 보충한다. 요청마다 토큰 1개를 소비하며, 소진 시 거부한다.
 *
 * <p>per-instance 라 단일 인스턴스 남용을 막는다. 다중 인스턴스 전역 한도는 Redis 백엔드 구현으로 교체.
 */
@Component
public class TokenBucketRateLimiter implements RateLimiter {

    private static final long MILLIS_PER_SECOND = 1000L;

    private final long capacity;
    private final double refillTokensPerMilli;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(
            @Value("${payment.rate-limit.capacity:60}") long capacity,
            @Value("${payment.rate-limit.refill-per-second:10}") double refillPerSecond) {
        this.capacity = capacity;
        this.refillTokensPerMilli = refillPerSecond / MILLIS_PER_SECOND;
    }

    @Override
    public boolean tryAcquire(String key) {
        return buckets.computeIfAbsent(key, k -> new Bucket(capacity, refillTokensPerMilli)).tryConsume();
    }

    /** 키 1개의 토큰 버킷. 마지막 보충 시각으로부터 경과 시간에 비례해 토큰을 채운다(상한 capacity). */
    private static final class Bucket {

        private final long capacity;
        private final double refillTokensPerMilli;
        private double tokens;
        private long lastRefillMillis;

        Bucket(long capacity, double refillTokensPerMilli) {
            this.capacity = capacity;
            this.refillTokensPerMilli = refillTokensPerMilli;
            this.tokens = capacity;
            this.lastRefillMillis = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            tokens = Math.min(capacity, tokens + (now - lastRefillMillis) * refillTokensPerMilli);
            lastRefillMillis = now;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
