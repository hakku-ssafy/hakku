package com.hakku.payment.payment;

/**
 * 결제 도메인 Kafka 토픽명. 브로커가 토픽을 자동 생성(auto-create)하므로, 오타난 토픽명은 조용히 유령 토픽을
 * 만들고 이벤트를 삼킨다. 이를 막기 위해 토픽 문자열을 한곳에서 상수로 관리하고 발행자/소비자가 공유한다.
 */
public final class PaymentTopics {

    public static final String APPROVED = "payment.approved";
    public static final String FAILED = "payment.failed";

    private PaymentTopics() {
    }
}
