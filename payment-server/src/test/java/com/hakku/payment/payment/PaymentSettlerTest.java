package com.hakku.payment.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.payment.gateway.GatewayResult;
import com.hakku.payment.outbox.OutboxRecorder;
import com.hakku.payment.payment.domain.Payment;
import com.hakku.payment.payment.domain.PaymentStatus;
import com.hakku.payment.payment.exception.PaymentNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 공유 정산 단위({@link PaymentSettler})를 검증한다(DB 없이 Mockito): 승인/실패 확정 + 아웃박스 기록,
 * 그리고 이미 종료된 결제에 대한 멱등 no-op(웹훅 재전송/이중 정산 대비).
 */
@ExtendWith(MockitoExtension.class)
class PaymentSettlerTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OutboxRecorder outboxRecorder;

    private PaymentSettler paymentSettler;

    @BeforeEach
    void setUp() {
        paymentSettler = new PaymentSettler(paymentRepository, outboxRecorder);
    }

    private Payment pending() {
        return new Payment(1L, "CART", "cart-42", 15000L, "KRW", "MOCK", "idem-1");
    }

    @Test
    @DisplayName("settles a PENDING payment to APPROVED and records an APPROVED event")
    void approvesPendingAndRecords() {
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(pending()));

        Payment settled = paymentSettler.settle(10L, GatewayResult.approved("MOCK-tx"));

        assertEquals(PaymentStatus.APPROVED, settled.getStatus());
        assertEquals("MOCK-tx", settled.getProviderTxId());
        verify(outboxRecorder)
                .record(eq("PAYMENT"), any(), eq(PaymentTopics.APPROVED), any(PaymentEvent.class));
    }

    @Test
    @DisplayName("settles a PENDING payment to FAILED and records a FAILED event")
    void failsPendingAndRecords() {
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(pending()));

        Payment settled = paymentSettler.settle(10L, GatewayResult.failed("INSUFFICIENT_FUNDS"));

        assertEquals(PaymentStatus.FAILED, settled.getStatus());
        verify(outboxRecorder)
                .record(eq("PAYMENT"), any(), eq(PaymentTopics.FAILED), any(PaymentEvent.class));
    }

    @Test
    @DisplayName("an already-settled payment is an idempotent no-op (no state change, no event)")
    void alreadyTerminalIsIdempotentNoop() {
        Payment alreadyApproved = pending();
        alreadyApproved.approve("MOCK-tx");
        when(paymentRepository.findById(10L)).thenReturn(Optional.of(alreadyApproved));

        Payment settled = paymentSettler.settle(10L, GatewayResult.failed("late-webhook"));

        assertEquals(PaymentStatus.APPROVED, settled.getStatus());
        verify(outboxRecorder, never()).record(any(), any(), any(), any());
    }

    @Test
    @DisplayName("settling a missing payment throws PaymentNotFoundException")
    void missingPaymentThrows() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class,
                () -> paymentSettler.settle(99L, GatewayResult.approved("MOCK-tx")));
    }
}
