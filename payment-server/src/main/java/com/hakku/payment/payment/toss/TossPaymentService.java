package com.hakku.payment.payment.toss;

import com.hakku.payment.gateway.GatewayResult;
import com.hakku.payment.gateway.toss.TossPaymentClient;
import com.hakku.payment.payment.PaymentRepository;
import com.hakku.payment.payment.PaymentSettler;
import com.hakku.payment.payment.domain.Payment;
import com.hakku.payment.payment.domain.PaymentStatus;
import com.hakku.payment.payment.exception.PaymentNotFoundException;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토스 결제위젯 결제 흐름 오케스트레이터.
 *
 * <p><b>prepare</b>(intent-first): 위젯 표시 전, 서버 기준 금액으로 PENDING 결제를 선커밋하고 orderId 를 발급한다.
 * orderId 는 서버가 만든 고유값이라 멱등 키로 그대로 쓴다(클라 멱등키 충돌·이중 INSERT 레이스가 구조적으로 없음).
 *
 * <p><b>confirm</b>: 토스 successUrl 이후 프론트가 호출. (1) orderId 로 PENDING 조회 + 소유자 확인,
 * (2) 저장 금액 vs 토스 반환 금액 일치 검증(위변조 방지), (3) 토스 승인 API 호출(트랜잭션 <b>밖</b> — 블로킹 I/O
 * 동안 커넥션 점유 회피), (4) 공유 {@link PaymentSettler} 로 APPROVED/FAILED 확정 + 아웃박스. 종료 결제는 멱등 no-op.
 *
 * <p>승인 결과를 {@link GatewayResult} 로 받아 동기/웹훅과 동일한 정산 경로를 재사용한다.
 */
@Service
public class TossPaymentService {

    private static final String PROVIDER = "TOSS";
    private static final String DEFAULT_CURRENCY = "KRW";

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final PaymentSettler paymentSettler;

    public TossPaymentService(PaymentRepository paymentRepository,
                              TossPaymentClient tossPaymentClient,
                              PaymentSettler paymentSettler) {
        this.paymentRepository = paymentRepository;
        this.tossPaymentClient = tossPaymentClient;
        this.paymentSettler = paymentSettler;
    }

    @Transactional
    public TossPrepareResponse prepare(Long userId, TossPrepareRequest request) {
        String orderId = generateOrderId();
        String currency = (request.currency() == null || request.currency().isBlank())
                ? DEFAULT_CURRENCY : request.currency();
        Payment payment = paymentRepository.saveAndFlush(new Payment(
                userId, request.referenceType(), request.referenceId(),
                request.amount(), currency, PROVIDER, orderId));
        return new TossPrepareResponse(orderId, payment.getAmount(), currency);
    }

    public TossConfirmResponse confirm(Long userId, TossConfirmRequest request) {
        Payment payment = paymentRepository.findByIdempotencyKey(request.orderId())
                .filter(p -> p.getUserId().equals(userId)) // 소유자 불일치는 '없음'으로 — 존재 누설 방지
                .orElseThrow(() -> new PaymentNotFoundException("결제를 찾을 수 없습니다."));
        if (payment.getAmount() != request.amount()) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return TossConfirmResponse.from(payment); // 멱등 재요청 → 기존 상태
        }
        GatewayResult result = tossPaymentClient.confirm(
                request.paymentKey(), request.orderId(), request.amount());
        return TossConfirmResponse.from(settleWithRetry(payment.getId(), result));
    }

    /** 동기/웹훅과의 동시 정산에서 낙관적 락 패자는 1회 재시도 → 종료 상태가 보여 멱등 no-op 이 된다. */
    private Payment settleWithRetry(Long paymentId, GatewayResult result) {
        try {
            return paymentSettler.settle(paymentId, result);
        } catch (ObjectOptimisticLockingFailureException race) {
            return paymentSettler.settle(paymentId, result);
        }
    }

    /** 토스 orderId 규격(영문/숫자/-/_ 6~64자)을 만족하는 서버 생성 고유 주문번호. */
    private String generateOrderId() {
        return "order_" + UUID.randomUUID().toString().replace("-", "");
    }
}
