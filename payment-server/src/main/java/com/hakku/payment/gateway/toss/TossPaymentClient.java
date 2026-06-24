package com.hakku.payment.gateway.toss;

import com.hakku.payment.gateway.GatewayResult;

/**
 * 토스페이먼츠 결제 승인 포트. v2 결제위젯 흐름에서 프론트가 받은 {@code paymentKey} 로 서버가 결제를 최종 승인한다.
 *
 * <p>결과를 {@link GatewayResult} 로 돌려주어 동기/웹훅과 동일한 {@code PaymentSettler} 정산 경로를 재사용한다:
 * 승인(2xx) → {@code approved(paymentKey)}, 거절(4xx) → {@code failed(code)}. 통신 자체가 실패(5xx/네트워크)하면
 * 결제 성공 여부가 <b>미정</b>이므로 정산하지 않도록 {@link TossApiUnavailableException} 을 던진다.
 */
public interface TossPaymentClient {

    GatewayResult confirm(String paymentKey, String orderId, long amount);
}
