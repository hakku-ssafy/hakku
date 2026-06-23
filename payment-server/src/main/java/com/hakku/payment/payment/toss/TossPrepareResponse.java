package com.hakku.payment.payment.toss;

/**
 * prepare 응답. 프론트는 발급된 {@code orderId} 로 토스 위젯 {@code requestPayment()} 를 호출하고,
 * {@code amount} 로 {@code setAmount()} 를 맞춘다. (clientKey 는 프론트 환경변수에서 따로 주입한다.)
 */
public record TossPrepareResponse(String orderId, long amount, String currency) {
}
