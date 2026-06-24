package com.hakku.payment.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.payment.gateway.GatewayChargeRequest;
import com.hakku.payment.gateway.GatewayResult;
import com.hakku.payment.gateway.PaymentGateway;
import com.hakku.payment.payment.domain.Payment;
import com.hakku.payment.payment.domain.PaymentStatus;
import com.hakku.payment.payment.exception.IdempotencyConflictException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * 결제 요청의 intent-first 멱등 오케스트레이션을 검증한다(DB 없이 Mockito).
 * 의도 선커밋→과금→정산 위임, 빠른 경로/409, 동시 레이스 catch-refetch, <b>과금 실패 시 PENDING 유지(롤백 금지)</b>,
 * 정산 낙관적 락 재시도를 다룬다.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentIntentRecorder intentRecorder;
    @Mock
    private PaymentGateway paymentGateway;
    @Mock
    private PaymentSettler paymentSettler;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository, intentRecorder, paymentGateway, paymentSettler);
    }

    private PaymentRequest request() {
        return new PaymentRequest("CART", "cart-42", 15000L, "KRW", "idem-1");
    }

    private Payment pendingPayment() {
        return new Payment(1L, "CART", "cart-42", 15000L, "KRW", "MOCK", "idem-1");
    }

    private Payment approvedPayment() {
        Payment payment = pendingPayment();
        payment.approve("MOCK-tx");
        return payment;
    }

    @Test
    @DisplayName("a new request records intent, charges, then settles and returns the settled result")
    void newRequestRecordsChargesSettles() {
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(intentRecorder.record(eq(1L), any(PaymentRequest.class))).thenReturn(pendingPayment());
        when(paymentGateway.charge(any(GatewayChargeRequest.class)))
                .thenReturn(GatewayResult.approved("MOCK-tx"));
        when(paymentSettler.settle(any(), any())).thenReturn(approvedPayment());

        var response = paymentService.requestPayment(1L, request());

        assertEquals(PaymentStatus.APPROVED, response.status());
        assertEquals("MOCK-tx", response.providerTxId());
    }

    @Test
    @DisplayName("a duplicate idempotency key returns the existing payment without recording or charging")
    void duplicateReturnsExisting() {
        when(paymentRepository.findByIdempotencyKey("idem-1"))
                .thenReturn(Optional.of(approvedPayment()));

        var response = paymentService.requestPayment(1L, request());

        assertEquals(PaymentStatus.APPROVED, response.status());
        verify(intentRecorder, never()).record(any(), any());
        verify(paymentGateway, never()).charge(any());
    }

    @Test
    @DisplayName("reusing an idempotency key with a different amount is rejected without charging")
    void duplicateKeyDifferentAmountConflicts() {
        when(paymentRepository.findByIdempotencyKey("idem-1"))
                .thenReturn(Optional.of(approvedPayment()));
        var differentAmount = new PaymentRequest("CART", "cart-42", 99999L, "KRW", "idem-1");

        assertThrows(IdempotencyConflictException.class,
                () -> paymentService.requestPayment(1L, differentAmount));
        verify(intentRecorder, never()).record(any(), any());
    }

    @Test
    @DisplayName("a concurrent race loser refetches and returns the winner's payment instead of failing")
    void concurrentRaceLoserRefetchesWinner() {
        when(paymentRepository.findByIdempotencyKey("idem-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(approvedPayment()));
        when(intentRecorder.record(eq(1L), any(PaymentRequest.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation: idempotency_key"));

        var response = paymentService.requestPayment(1L, request());

        assertEquals(PaymentStatus.APPROVED, response.status());
        verify(paymentGateway, never()).charge(any());
    }

    @Test
    @DisplayName("when the gateway charge call throws, the intent stays PENDING (no rollback, no settle)")
    void chargeFailureLeavesPaymentPending() {
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(intentRecorder.record(eq(1L), any(PaymentRequest.class))).thenReturn(pendingPayment());
        when(paymentGateway.charge(any(GatewayChargeRequest.class)))
                .thenThrow(new RuntimeException("PG timeout"));

        var response = paymentService.requestPayment(1L, request());

        assertEquals(PaymentStatus.PENDING, response.status());
        verify(paymentSettler, never()).settle(any(), any());
    }

    @Test
    @DisplayName("a concurrent webhook settling first causes an optimistic-lock retry that resolves idempotently")
    void optimisticLockOnSettleRetries() {
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(intentRecorder.record(eq(1L), any(PaymentRequest.class))).thenReturn(pendingPayment());
        when(paymentGateway.charge(any(GatewayChargeRequest.class)))
                .thenReturn(GatewayResult.approved("MOCK-tx"));
        when(paymentSettler.settle(any(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Payment.class, 1L))
                .thenReturn(approvedPayment());

        var response = paymentService.requestPayment(1L, request());

        assertEquals(PaymentStatus.APPROVED, response.status());
    }
}
