package com.hakku.payment.gateway;

/**
 * PG 승인/실패 결과. 정적 팩터리로만 생성해 잘못된 조합(승인인데 실패 사유, 실패인데 거래 id)을 막는다.
 */
public record GatewayResult(boolean approved, String providerTxId, String failureReason) {

    /** 승인 결과. PG 거래 식별자는 필수. */
    public static GatewayResult approved(String providerTxId) {
        if (providerTxId == null || providerTxId.isBlank()) {
            throw new IllegalArgumentException("승인 결과에는 PG 거래 id 가 필요합니다.");
        }
        return new GatewayResult(true, providerTxId, null);
    }

    /** 실패 결과. 사유는 필수. */
    public static GatewayResult failed(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("실패 결과에는 사유가 필요합니다.");
        }
        return new GatewayResult(false, null, failureReason);
    }
}
