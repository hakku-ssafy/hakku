package com.hakku.payment.gateway.toss;

/**
 * 토스 결제 승인 API 통신 자체가 실패(5xx/네트워크/타임아웃)했을 때 던진다. 결제 성공 여부가 <b>미정</b>이라
 * 임의로 APPROVED/FAILED 로 정산하면 안 되므로(이중과금/유실 위험), PENDING 을 유지하고 호출자에게 재시도/오류를
 * 알린다. {@code GlobalExceptionHandler} 가 502(Bad Gateway)로 매핑한다.
 */
public class TossApiUnavailableException extends RuntimeException {

    public TossApiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
