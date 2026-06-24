package com.hakku.payment.webhook;

import com.hakku.payment.gateway.GatewayResult;
import com.hakku.payment.payment.PaymentRepository;
import com.hakku.payment.payment.PaymentSettler;
import com.hakku.payment.payment.domain.Payment;
import com.hakku.payment.payment.exception.PaymentNotFoundException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * PG 웹훅을 결제 정산으로 반영한다. 동기 요청 경로와 <b>동일한</b> {@link PaymentSettler} 를 호출하므로 정산은 멱등이다
 * (이미 종료된 결제면 no-op → PG 웹훅 재전송에도 안전). 동시 정산(동기 경로 vs 웹훅)의 낙관적 락 충돌은 1회 재시도로 흡수한다.
 *
 * <p>이 경로가 intent-first 와 짝을 이뤄 과금-성공-후-커밋-실패 갭을 닫는다: 내구성 있게 커밋된 PENDING 을 웹훅이 찾아 확정한다.
 */
@Service
public class PaymentWebhookService {

    private static final String OUTCOME_APPROVED = "APPROVED";
    private static final String OUTCOME_FAILED = "FAILED";

    private final PaymentRepository paymentRepository;
    private final PaymentSettler paymentSettler;

    public PaymentWebhookService(PaymentRepository paymentRepository, PaymentSettler paymentSettler) {
        this.paymentRepository = paymentRepository;
        this.paymentSettler = paymentSettler;
    }

    public void apply(PaymentWebhookRequest request) {
        Payment payment = paymentRepository.findByIdempotencyKey(request.idempotencyKey())
                .orElseThrow(() -> new PaymentNotFoundException("웹훅 대상 결제를 찾을 수 없습니다."));
        settleWithRetry(payment.getId(), toGatewayResult(request));
    }

    /**
     * 웹훅 outcome 을 PG 결과로 변환한다. 알 수 없는/누락된 outcome 은 조용히 FAILED 로 떨어뜨리지 않고
     * {@link IllegalArgumentException}(→400)으로 거부한다 — 결제를 잘못 FAILED 로 영구 확정하는 사고를 막는다.
     */
    private GatewayResult toGatewayResult(PaymentWebhookRequest request) {
        if (request.outcome() == null) {
            throw new IllegalArgumentException("웹훅 outcome 필드가 없습니다.");
        }
        return switch (request.outcome()) {
            case OUTCOME_APPROVED -> GatewayResult.approved(request.providerTxId());
            case OUTCOME_FAILED -> GatewayResult.failed(
                    (request.failureReason() == null || request.failureReason().isBlank())
                            ? "PG_DECLINED" : request.failureReason());
            default -> throw new IllegalArgumentException("알 수 없는 webhook outcome: " + request.outcome());
        };
    }

    private void settleWithRetry(Long paymentId, GatewayResult result) {
        try {
            paymentSettler.settle(paymentId, result);
        } catch (ObjectOptimisticLockingFailureException raced) {
            // 동기 경로가 동시에 정산함 → 재조회하면 종료 상태 → 멱등 no-op 으로 흡수.
            paymentSettler.settle(paymentId, result);
        }
    }
}
