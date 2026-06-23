package com.hakku.payment.payment.toss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hakku.payment.gateway.GatewayResult;
import com.hakku.payment.gateway.toss.TossPaymentClient;
import com.hakku.payment.payment.PaymentRepository;
import com.hakku.payment.payment.PaymentSettler;
import com.hakku.payment.payment.domain.Payment;
import com.hakku.payment.payment.domain.PaymentStatus;
import com.hakku.payment.payment.exception.PaymentNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * 토스 결제 흐름 오케스트레이션 검증(DB/네트워크 없이 Mockito). 정산 자체의 정확성은 {@code PaymentSettlerTest},
 * HTTP 매핑은 {@code RestClientTossPaymentClientTest} 가 담당하므로, 여기서는 prepare 선커밋과 confirm 의
 * 소유/금액 검증·멱등·재시도·토스 호출 여부에 집중한다.
 */
@ExtendWith(MockitoExtension.class)
class TossPaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TossPaymentClient tossPaymentClient;
    @Mock
    private PaymentSettler paymentSettler;

    private TossPaymentService service;

    @BeforeEach
    void setUp() {
        service = new TossPaymentService(paymentRepository, tossPaymentClient, paymentSettler);
    }

    private Payment pending(Long userId, long amount) {
        return new Payment(userId, "PRODUCT", "42", amount, "KRW", "TOSS", "order_seed");
    }

    @Test
    @DisplayName("prepare 는 provider=TOSS PENDING 을 저장하고 발급 orderId 를 멱등키로 쓴다")
    void prepareSavesPendingTossIntent() {
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        TossPrepareResponse res = service.prepare(7L, new TossPrepareRequest("PRODUCT", "42", 15000L, "KRW"));

        assertTrue(res.orderId().startsWith("order_"));
        assertEquals(15000L, res.amount());
        assertEquals("KRW", res.currency());

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(captor.capture());
        Payment saved = captor.getValue();
        assertEquals("TOSS", saved.getProvider());
        assertEquals(PaymentStatus.PENDING, saved.getStatus());
        assertEquals(res.orderId(), saved.getIdempotencyKey());
        assertEquals(7L, saved.getUserId());
    }

    @Test
    @DisplayName("currency 미지정이면 KRW 로 기본 설정")
    void prepareDefaultsCurrency() {
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        TossPrepareResponse res = service.prepare(7L, new TossPrepareRequest("PRODUCT", "42", 15000L, null));
        assertEquals("KRW", res.currency());
    }

    @Test
    @DisplayName("confirm: 토스 승인이면 APPROVED 로 정산한다")
    void confirmApproves() {
        Payment pending = pending(7L, 15000L);
        Payment approved = pending(7L, 15000L);
        approved.approve("pk_1");
        when(paymentRepository.findByIdempotencyKey("order_1")).thenReturn(Optional.of(pending));
        when(tossPaymentClient.confirm("pk_1", "order_1", 15000L)).thenReturn(GatewayResult.approved("pk_1"));
        when(paymentSettler.settle(any(), any())).thenReturn(approved);

        TossConfirmResponse res = service.confirm(7L, new TossConfirmRequest("pk_1", "order_1", 15000L));

        assertEquals("APPROVED", res.status());
        verify(tossPaymentClient).confirm("pk_1", "order_1", 15000L);
        verify(paymentSettler).settle(any(), any());
    }

    @Test
    @DisplayName("confirm: 저장 금액과 다르면 토스 호출 없이 거부(위변조 방지)")
    void confirmRejectsAmountMismatch() {
        when(paymentRepository.findByIdempotencyKey("order_1")).thenReturn(Optional.of(pending(7L, 15000L)));

        assertThrows(IllegalArgumentException.class,
                () -> service.confirm(7L, new TossConfirmRequest("pk_1", "order_1", 99999L)));

        verify(tossPaymentClient, never()).confirm(any(), any(), anyLong());
        verify(paymentSettler, never()).settle(any(), any());
    }

    @Test
    @DisplayName("confirm: 다른 사용자 결제는 '없음'으로 처리(존재 누설 방지)")
    void confirmRejectsForeignOwner() {
        when(paymentRepository.findByIdempotencyKey("order_1")).thenReturn(Optional.of(pending(7L, 15000L)));

        assertThrows(PaymentNotFoundException.class,
                () -> service.confirm(8L, new TossConfirmRequest("pk_1", "order_1", 15000L)));
        verify(tossPaymentClient, never()).confirm(any(), any(), anyLong());
    }

    @Test
    @DisplayName("confirm: 미지의 orderId 는 PaymentNotFoundException")
    void confirmUnknownOrder() {
        when(paymentRepository.findByIdempotencyKey("order_x")).thenReturn(Optional.empty());
        assertThrows(PaymentNotFoundException.class,
                () -> service.confirm(7L, new TossConfirmRequest("pk_1", "order_x", 15000L)));
    }

    @Test
    @DisplayName("confirm: 이미 종료된 결제 재요청은 토스 호출 없이 멱등 반환")
    void confirmReplayIsIdempotent() {
        Payment approved = pending(7L, 15000L);
        approved.approve("pk_1");
        when(paymentRepository.findByIdempotencyKey("order_1")).thenReturn(Optional.of(approved));

        TossConfirmResponse res = service.confirm(7L, new TossConfirmRequest("pk_1", "order_1", 15000L));

        assertEquals("APPROVED", res.status());
        verify(tossPaymentClient, never()).confirm(any(), any(), anyLong());
        verify(paymentSettler, never()).settle(any(), any());
    }

    @Test
    @DisplayName("confirm: 토스 거절이면 FAILED 로 정산한다")
    void confirmDeclineFails() {
        Payment pending = pending(7L, 15000L);
        Payment failed = pending(7L, 15000L);
        failed.fail();
        when(paymentRepository.findByIdempotencyKey("order_1")).thenReturn(Optional.of(pending));
        when(tossPaymentClient.confirm("pk_1", "order_1", 15000L)).thenReturn(GatewayResult.failed("REJECT_CARD_COMPANY"));
        when(paymentSettler.settle(any(), any())).thenReturn(failed);

        TossConfirmResponse res = service.confirm(7L, new TossConfirmRequest("pk_1", "order_1", 15000L));

        assertEquals("FAILED", res.status());
    }

    @Test
    @DisplayName("confirm: 동시 정산 낙관적 락 충돌은 1회 재시도한다")
    void confirmRetriesOnOptimisticLock() {
        Payment pending = pending(7L, 15000L);
        Payment approved = pending(7L, 15000L);
        approved.approve("pk_1");
        when(paymentRepository.findByIdempotencyKey("order_1")).thenReturn(Optional.of(pending));
        when(tossPaymentClient.confirm("pk_1", "order_1", 15000L)).thenReturn(GatewayResult.approved("pk_1"));
        when(paymentSettler.settle(any(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Payment.class, 1L))
                .thenReturn(approved);

        TossConfirmResponse res = service.confirm(7L, new TossConfirmRequest("pk_1", "order_1", 15000L));

        assertEquals("APPROVED", res.status());
        verify(paymentSettler, times(2)).settle(any(), any());
    }
}
