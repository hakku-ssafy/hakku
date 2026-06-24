package com.hakku.payment.common.web;

import com.hakku.payment.gateway.toss.TossApiUnavailableException;
import com.hakku.payment.payment.exception.IdempotencyConflictException;
import com.hakku.payment.payment.exception.PaymentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    /**
     * 토스 결제 승인 API 통신 실패(5xx/네트워크) → 502. 결제 성공 여부가 미정이므로 임의 정산하지 않고
     * 게이트웨이 오류로 알린다. 세부는 서버 로그에만 남기고 클라이언트엔 일반화 메시지를 준다.
     */
    @ExceptionHandler(TossApiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleTossUnavailable(TossApiUnavailableException ex) {
        log.error("토스 결제 승인 통신 실패", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("결제 승인 처리가 지연되고 있습니다. 잠시 후 다시 시도해 주세요."));
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

    /** 요청 본문 JSON 파싱 실패 → 400. 아래 catch-all(500)이 이를 흡수하지 않도록 명시 매핑한다. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("요청 본문을 해석할 수 없습니다."));
    }

    /**
     * M-4: 동시 정산 writer(동기 경로 vs PG 웹훅) 간 낙관적 락 충돌이 1회 재시도 후에도 남으면 → 409.
     * 클라이언트/PG 에 재시도 가능 신호를 주어 500 누수를 막는다(정상 멱등 충돌은 종료상태 재조회로 흡수돼 여기 안 옴).
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("낙관적 락 충돌이 재시도 후에도 해소되지 않음", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("결제 상태가 동시에 변경되었습니다. 잠시 후 다시 시도해 주세요."));
    }

    /**
     * M-4: 위에서 매핑되지 않은 미처리 런타임 예외의 최종 안전망 → 500. 내부 메시지/스택은 서버 로그에만 남기고
     * 클라이언트에는 일반화 메시지만 준다(정보 노출 방지). 단일 {@link ErrorResponse} 형태를 보장한다.
     *
     * <p><b>{@code RuntimeException} 으로 한정</b>한 이유: Spring MVC 의 4xx 프레임워크 예외 다수(예:
     * {@code NoResourceFoundException}→404, 메서드 미지원→405, 미디어타입→415)는 {@code ServletException}
     * (checked) 라 여기에 걸리지 않고 Spring 기본 매핑(정상 4xx)으로 흐른다 — catch-all 이 정당한 404 를 500 으로
     * 삼키지 않게 한다. JSON 파싱 실패는 위 {@code HttpMessageNotReadableException} 핸들러가 400 으로 잡는다.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(RuntimeException ex, HttpServletRequest request) {
        log.error("미처리 예외: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
    }
}
