package com.hakku.payment.payment.toss;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 토스 결제위젯 API.
 *
 * <p><b>소유 기반 인가</b>: {@code userId} 는 JWT subject 에서만 온다(바디로 받지 않음). 경로가
 * {@code /api/payments/...} 아래라 nginx 가 payment-server 로 라우팅하며, 웹훅(permitAll)과 달리 JWT 인증이 필요하다.
 */
@RestController
@RequestMapping("/api/payments/toss")
public class TossPaymentController {

    private final TossPaymentService tossPaymentService;

    public TossPaymentController(TossPaymentService tossPaymentService) {
        this.tossPaymentService = tossPaymentService;
    }

    /** 위젯 표시 전 PENDING 의도 선커밋 + orderId 발급. */
    @PostMapping("/prepare")
    public TossPrepareResponse prepare(@AuthenticationPrincipal String userId,
                                       @Valid @RequestBody TossPrepareRequest request) {
        return tossPaymentService.prepare(Long.valueOf(userId), request);
    }

    /** successUrl 이후 결제 최종 승인(토스 confirm 호출 + 정산). */
    @PostMapping("/confirm")
    public TossConfirmResponse confirm(@AuthenticationPrincipal String userId,
                                       @Valid @RequestBody TossConfirmRequest request) {
        return tossPaymentService.confirm(Long.valueOf(userId), request);
    }
}
