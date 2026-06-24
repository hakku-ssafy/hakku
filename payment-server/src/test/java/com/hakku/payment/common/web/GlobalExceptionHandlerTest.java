package com.hakku.payment.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.hakku.payment.common.web.GlobalExceptionHandler.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * M-4: 낙관적 락 충돌이 재시도 후에도 남으면 500 으로 누수되던 것을 409 로 매핑하고,
 * 미처리 예외는 일반화된 단일 ErrorResponse 500 으로 내려(내부 메시지 비노출) 처리한다.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("재시도 후에도 남은 낙관적 락 충돌 → 409 (재시도 가능 신호)")
    void optimisticLockMapsToConflict() {
        ResponseEntity<ErrorResponse> response =
                handler.handleOptimisticLock(new ObjectOptimisticLockingFailureException("Payment", 1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isNotBlank();
    }

    @Test
    @DisplayName("미처리 예외 → 500 + 내부 상세를 노출하지 않는 일반화 메시지")
    void unexpectedExceptionMapsToGenericInternalError() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new RuntimeException("internal-leak-xyz"), new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).doesNotContain("internal-leak-xyz");
    }
}
