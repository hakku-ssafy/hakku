package com.hakku.payment.payment.toss;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * 토스 결제위젯 표시 전 결제 의도 선등록 요청. 도메인 비종속 — "참조 R 에 대한 금액 X".
 * currency 는 선택(미지정 시 KRW). 금액은 위변조 방지를 위해 서버가 저장하고 confirm 에서 토스 금액과 대조한다.
 */
public record TossPrepareRequest(
        @NotBlank String referenceType,
        @NotBlank String referenceId,
        @Positive long amount,
        String currency) {
}
