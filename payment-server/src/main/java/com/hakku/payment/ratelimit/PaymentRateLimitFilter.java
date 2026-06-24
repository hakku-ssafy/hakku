package com.hakku.payment.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * H-4: 결제 엔드포인트({@code /api/payments/**}) 레이트리밋 필터. 인증 사용자(JWT subject) 또는 클라이언트 IP
 * 단위로 토큰 버킷을 적용해 prepare/confirm/webhook 남용(PENDING 양산·orderId 탐지·웹훅 DoS)을 제한한다.
 * 한도 초과 시 본문만 일반화한 429 로 거부한다.
 *
 * <p>SecurityConfig 가 JWT 필터 직후(익명 인증 설정 전)에 등록하므로, 인증된 결제 요청은 subject 키로,
 * 토큰 없는 웹훅은 IP 키로 묶인다.
 */
public class PaymentRateLimitFilter extends OncePerRequestFilter {

    private static final String PAYMENTS_PREFIX = "/api/payments";
    private static final String ANONYMOUS = "anonymousUser";

    private final RateLimiter rateLimiter;
    private final boolean enabled;

    public PaymentRateLimitFilter(RateLimiter rateLimiter, boolean enabled) {
        this.rateLimiter = rateLimiter;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!enabled || !request.getRequestURI().startsWith(PAYMENTS_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!rateLimiter.tryAcquire(clientKey(request))) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"요청이 너무 잦습니다. 잠시 후 다시 시도해 주세요.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String userId && !ANONYMOUS.equals(userId)) {
            return "user:" + userId;
        }
        return "ip:" + clientIp(request);
    }

    private static String clientIp(HttpServletRequest request) {
        // 신뢰 가능한 프록시(nginx)가 설정하는 X-Real-IP($remote_addr)만 신뢰한다. 클라이언트가 위조 가능한
        // X-Forwarded-For 는 레이트리밋 키로 쓰지 않는다(스푸핑으로 IP 버킷 우회 방지).
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
