package com.hakku.payment.payment;

import com.hakku.payment.gateway.GatewayResult;
import com.hakku.payment.outbox.OutboxRecorder;
import com.hakku.payment.payment.domain.Payment;
import com.hakku.payment.payment.domain.PaymentStatus;
import com.hakku.payment.payment.exception.PaymentNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 <b>정산(settlement)</b>의 원자적 트랜잭션 단위: PENDING 결제를 PG 결과에 따라 APPROVED/FAILED 로 확정하고
 * 같은 트랜잭션에서 아웃박스 이벤트를 기록한다(상태+이벤트 원자적 커밋 → 이중 쓰기 방지).
 * 동기 요청 경로({@link PaymentService})와 PG 웹훅 경로가 <b>공유</b>하는 단일 정산 지점이다.
 *
 * <p><b>멱등</b>: 이미 종료 상태면 아무 것도 하지 않는다(PG 웹훅 재전송, 동기/웹훅 이중 정산 대비) — 아웃박스 이벤트도
 * 전이가 일어난 1회만 기록된다.
 *
 * <p><b>동시성</b>: {@code Payment} 의 {@code @Version} 낙관적 락이 두 정산 writer 의 동시 전이를 막는다. 패자의 커밋은
 * {@code ObjectOptimisticLockingFailureException} 으로 실패하고, 호출자가 재시도하면 종료 상태가 보여 멱등 no-op 이 된다.
 * 이 단위를 별도 빈으로 둔 이유: (1) {@code @Transactional} 프록시가 실제로 적용되고, (2) 낙관적 락 예외는 커밋 시점
 * (메서드 반환 이후)에 던져지므로 호출자가 트랜잭션 <b>밖에서</b> 잡아 재시도할 수 있다.
 */
@Component
public class PaymentSettler {

    private static final String AGGREGATE_TYPE = "PAYMENT";

    private final PaymentRepository paymentRepository;
    private final OutboxRecorder outboxRecorder;

    public PaymentSettler(PaymentRepository paymentRepository, OutboxRecorder outboxRecorder) {
        this.paymentRepository = paymentRepository;
        this.outboxRecorder = outboxRecorder;
    }

    @Transactional
    public Payment settle(Long paymentId, GatewayResult result) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("정산 대상 결제를 찾을 수 없습니다."));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return payment; // 이미 정산됨 → 멱등 no-op (이벤트 미기록)
        }
        if (result.approved()) {
            payment.approve(result.providerTxId());
            outboxRecorder.record(AGGREGATE_TYPE, payment.getId(),
                    PaymentTopics.APPROVED, PaymentEvent.from(payment));
        } else {
            payment.fail();
            outboxRecorder.record(AGGREGATE_TYPE, payment.getId(),
                    PaymentTopics.FAILED, PaymentEvent.from(payment));
        }
        return payment;
    }
}
