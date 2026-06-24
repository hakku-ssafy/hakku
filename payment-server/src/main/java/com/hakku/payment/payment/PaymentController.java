package com.hakku.payment.payment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 API.
 *
 * <p><b>소유 기반 인가</b>: 인증 주체(JWT subject = 회원 id)가 곧 결제 주체다. {@code userId} 는 토큰에서만 오고
 * 요청 바디로는 받지 않는다 — 따라서 한 사용자가 다른 사용자 명의로 결제를 만들 수 없다. 멱등 처리/충돌/동시 레이스
 * 복구는 {@link PaymentService} 가 담당하며, 충돌(409)·검증 실패(400)는 {@code GlobalExceptionHandler} 가 매핑한다.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@AuthenticationPrincipal String userId,
                               @Valid @RequestBody PaymentRequest request) {
        return paymentService.requestPayment(Long.valueOf(userId), request);
    }
}
