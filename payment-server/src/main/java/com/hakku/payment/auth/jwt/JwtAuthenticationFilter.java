package com.hakku.payment.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청의 Bearer 토큰을 검증해 SecurityContext 에 인증 주체(회원 id)를 설정한다.
 * 토큰이 없거나 유효하지 않으면 컨텍스트를 비운 채 통과시키고, 인가(401)는 SecurityConfig 가 처리한다.
 * 결제는 역할 기반 인가를 쓰지 않으므로 권한 목록은 비운다 — 인증 여부만 본다. 주체(=userId)는
 * 컨트롤러에서 {@code @AuthenticationPrincipal String userId} 로 주입된다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length());
            // 리프레시 토큰(type=refresh)을 Bearer 액세스 토큰으로 쓰는 것을 거부한다 — 리프레시는 /api/auth 쿠키 전용.
            if (tokenProvider.validate(token) && !tokenProvider.isRefreshToken(token)) {
                String userId = tokenProvider.getSubject(token);
                if (isNumericUserId(userId)) {
                    var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 토큰 subject 가 숫자 회원 id 인지 확인한다. 결제 주체는 항상 숫자 userId 이므로 비숫자 subject
     * (서비스 계정/오설정 토큰 등)는 인증하지 않는다 — 컨트롤러의 {@code Long.valueOf} 가 500 으로 새는 것을
     * 막고, 인증 자료 검증을 필터의 책임으로 둔다(비숫자 → 컨텍스트 비움 → SecurityConfig 가 401).
     */
    private static boolean isNumericUserId(String subject) {
        if (subject == null || subject.isEmpty()) {
            return false;
        }
        try {
            Long.parseLong(subject);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
