package com.hakku.payment.ratelimit;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 인스턴스 로컬 토큰 버킷 레이트리밋(H-4). 키별로 {@code capacity} 만큼의 토큰을 두고 초당
 * {@code refill-per-second} 만큼 보충한다. 요청마다 토큰 1개를 소비하며, 소진 시 거부한다.
 *
 * <p>버킷 맵은 {@code max-keys} 로 상한을 두고 LRU 로 축출한다 — 위조 IP/대량 키로 인한 메모리 고갈(DoS)을 막는다.
 * per-instance 라 단일 인스턴스 남용을 막으며, 다중 인스턴스 전역 한도는 Redis 백엔드 구현으로 교체한다.
 */
@Component
public class TokenBucketRateLimiter implements RateLimiter {

    private static final long MILLIS_PER_SECOND = 1000L;

    private final long capacity;
    private final double refillTokensPerMilli;
    private final int maxKeys;
    private final LinkedHashMap<String, Bucket> buckets;

    public TokenBucketRateLimiter(
            @Value("${payment.rate-limit.capacity:60}") long capacity,
            @Value("${payment.rate-limit.refill-per-second:10}") double refillPerSecond,
            @Value("${payment.rate-limit.max-keys:100000}") int maxKeys) {
        this.capacity = capacity;
        this.refillTokensPerMilli = refillPerSecond / MILLIS_PER_SECOND;
        this.maxKeys = maxKeys;
        // accessOrder=true + removeEldestEntry → 가장 오래 쓰이지 않은 키부터 축출(상한 maxKeys).
        this.buckets = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
                return size() > TokenBucketRateLimiter.this.maxKeys;
            }
        };
    }

    @Override
    public boolean tryAcquire(String key) {
        Bucket bucket;
        // 맵 구조 변경(get 의 LRU 갱신 포함)만 락으로 보호하고, 토큰 소비는 버킷 자체 락에 맡긴다.
        synchronized (buckets) {
            bucket = buckets.get(key);
            if (bucket == null) {
                bucket = new Bucket(capacity, refillTokensPerMilli);
                buckets.put(key, bucket);
            }
        }
        return bucket.tryConsume();
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
