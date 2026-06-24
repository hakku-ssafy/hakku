package com.hakku.payment.payment;

import com.hakku.payment.gateway.PaymentGateway;
import com.hakku.payment.payment.domain.Payment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * intent-first 의 1단계: 결제 "의도"를 PENDING 으로 <b>먼저 커밋</b>한다(별도 트랜잭션). PG 과금 전에 내구성 있는 기록을
 * 남겨, 과금 직후 크래시/타임아웃이 나도 웹훅·정산이 이 PENDING 행을 찾아 확정할 수 있게 한다
 * (과금-성공-후-커밋-실패 갭 차단). 과금은 이 트랜잭션 <b>밖</b>에서 일어나므로 블로킹 PG I/O 동안 DB 커넥션을
 * 점유하지 않는다(Task4 H-1 과 같은 풀 고갈 안티패턴 회피).
 *
 * <p>{@code idempotency_key} UNIQUE 제약이 동시 레이스의 패자를 과금 전에 막는다 → 호출자가 catch-refetch 로 복구.
 */
@Component
public class PaymentIntentRecorder {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public PaymentIntentRecorder(PaymentRepository paymentRepository, PaymentGateway paymentGateway) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }

    @Transactional
    public Payment record(Long userId, PaymentRequest request) {
        return paymentRepository.saveAndFlush(new Payment(
                userId, request.referenceType(), request.referenceId(),
                request.amount(), request.currency(),
                paymentGateway.providerName(), request.idempotencyKey()));
    }
}
