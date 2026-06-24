package com.hakku.payment.payment;

import com.hakku.payment.gateway.GatewayChargeRequest;
import com.hakku.payment.gateway.GatewayResult;
import com.hakku.payment.gateway.PaymentGateway;
import com.hakku.payment.payment.domain.Payment;
import com.hakku.payment.payment.exception.IdempotencyConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * 결제 요청을 멱등하게 <b>오케스트레이션</b>한다(intent-first). 트랜잭션을 직접 열지 않고 세 단계로 나눠 호출한다:
 *
 * <ol>
 *   <li><b>의도 선커밋</b>: {@link PaymentIntentRecorder#record}(별도 tx)로 PENDING 을 내구성 있게 커밋. 동시 레이스의
 *       패자는 {@code idempotency_key} UNIQUE 위반({@link DataIntegrityViolationException}) → 승자 결제 재조회(catch-refetch).</li>
 *   <li><b>PG 과금</b>: 트랜잭션 <b>밖</b>에서 호출(블로킹 I/O 동안 DB 커넥션 점유 금지). 호출 실패/타임아웃이면 PENDING 을
 *       그대로 두고 미정 상태로 응답한다 — 절대 롤백하지 않는다(웹훅/정산이 최종 확정). 이것이 과금-성공-후-커밋-실패 갭을 닫는 핵심.</li>
 *   <li><b>정산</b>: {@link PaymentSettler#settle}(별도 tx, {@code @Version})로 APPROVED/FAILED 확정. 동시 웹훅 정산과의
 *       낙관적 락 충돌은 1회 재시도 → 종료 상태가 보여 멱등 no-op 으로 확정 결과를 반환한다.</li>
 * </ol>
 *
 * <p>빠른 경로: 동일 {@code idempotencyKey} 가 이미 있으면 기존 결제를 반환하고, 같은 키·다른 내용이면 409 로 거부한다.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentIntentRecorder intentRecorder;
    private final PaymentGateway paymentGateway;
    private final PaymentSettler paymentSettler;

    public PaymentService(PaymentRepository paymentRepository, PaymentIntentRecorder intentRecorder,
                          PaymentGateway paymentGateway, PaymentSettler paymentSettler) {
        this.paymentRepository = paymentRepository;
        this.intentRecorder = intentRecorder;
        this.paymentGateway = paymentGateway;
        this.paymentSettler = paymentSettler;
    }

    public PaymentResponse requestPayment(Long userId, PaymentRequest request) {
        Payment existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            return PaymentResponse.from(requireSameRequest(existing, userId, request));
        }

        Payment intent;
        try {
            intent = intentRecorder.record(userId, request);
        } catch (DataIntegrityViolationException raceLost) {
            return PaymentResponse.from(refetchWinner(userId, request, raceLost));
        }

        GatewayResult result;
        try {
            result = paymentGateway.charge(new GatewayChargeRequest(
                    intent.getAmount(), intent.getCurrency(),
                    intent.getReferenceType(), intent.getReferenceId(),
                    intent.getIdempotencyKey()));
        } catch (RuntimeException chargeError) {
            // PG 호출 실패/타임아웃: 의도(PENDING)는 이미 커밋됨. 과금이 실제로 됐는지 불명이므로 절대 롤백하지 않고
            // 미정(PENDING) 상태로 응답한다. 웹훅(또는 후속 정산)이 최종 상태를 확정한다.
            log.warn("PG 과금 호출 실패 — 결제 id={} 는 PENDING 유지(웹훅/정산이 확정): {}",
                    intent.getId(), chargeError.toString());
            return PaymentResponse.from(intent);
        }

        return PaymentResponse.from(settleWithRetry(intent.getId(), result));
    }

    /**
     * 정산을 시도하되, 동시 웹훅 정산과의 낙관적 락 충돌({@link ObjectOptimisticLockingFailureException})이면 1회 재시도한다.
     * 재시도 시 결제는 이미 종료 상태이므로 {@link PaymentSettler#settle} 가 멱등 no-op 으로 확정 결과를 돌려준다.
     */
    private Payment settleWithRetry(Long paymentId, GatewayResult result) {
        try {
            return paymentSettler.settle(paymentId, result);
        } catch (ObjectOptimisticLockingFailureException raced) {
            return paymentSettler.settle(paymentId, result);
        }
    }

    /** 멱등 빠른 경로: 같은 키·다른 내용은 충돌로 거부(키 재사용 방지). 일치하면 기존 결제를 그대로 반환한다. */
    private Payment requireSameRequest(Payment existing, Long userId, PaymentRequest request) {
        boolean sameRequest = existing.getUserId().equals(userId)
                && existing.getAmount() == request.amount()
                && existing.getCurrency().equals(request.currency())
                && existing.getReferenceType().equals(request.referenceType())
                && existing.getReferenceId().equals(request.referenceId());
        if (!sameRequest) {
            throw new IdempotencyConflictException(request.idempotencyKey());
        }
        return existing;
    }

    /** 동시 레이스 패자가 승자의 결제를 재조회한다. 의도 커밋은 이미 롤백됐으므로 이중 의도/과금은 없다. */
    private Payment refetchWinner(Long userId, PaymentRequest request, DataIntegrityViolationException cause) {
        Payment winner = paymentRepository.findByIdempotencyKey(request.idempotencyKey())
                .orElseThrow(() -> cause);
        return requireSameRequest(winner, userId, request);
    }
}
