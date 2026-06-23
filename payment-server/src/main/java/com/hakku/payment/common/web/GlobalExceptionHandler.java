package com.hakku.payment.common.web;

import com.hakku.payment.payment.exception.IdempotencyConflictException;
import com.hakku.payment.payment.exception.PaymentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 도메인/검증 예외를 HTTP 상태로 매핑한다. 클라이언트에는 일반화된 메시지만 노출하고 내부 스택/세부는 숨긴다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorResponse(String message) {
    }

    /** 같은 멱등 키로 내용이 다른 결제 요청(키 재사용) → 409. */
    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    /** 정산/조회 대상 결제 없음(예: 미지 참조의 웹훅) → 404. */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * 잘못된 요청 인자(예: 웹훅 outcome 누락/미지, 승인인데 providerTxId 누락) → 400.
     * PG 에게 <b>재시도 불가능한</b> 신호를 주어 무한 재전송(5xx)을 막는다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * 회복 불가한 무결성 위반(예: catch-refetch 가 승자 행을 못 찾는 비정상 상황) → 500.
     * 정상적인 멱등 충돌은 {@link IdempotencyConflictException}(409)로 다뤄지므로, 여기 도달하면 서버 이상이다.
     * 세부는 서버 로그에만 남기고 클라이언트에는 일반화 메시지만 반환한다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        log.error("회복 불가한 무결성 위반: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
    }

    /** 요청 바디 Bean Validation 실패 → 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message));
    }
}
