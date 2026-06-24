package com.hakku.payment.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 결제 애그리거트. 도메인 비종속 — "참조 R 에 대한 금액 X" 만 정산하며, 장바구니/주문 자체는 알지 못한다.
 *
 * <p>상태 머신: {@link PaymentStatus#PENDING} 만 비종료 상태이고 APPROVED/FAILED/CANCELLED 는 종료 상태다.
 * 종료 상태에서의 전이는 {@link IllegalStateException} 으로 거부한다.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 결제가 가리키는 외부 도메인 종류(예: "CART"). 결제 서버는 그 의미를 해석하지 않는다. */
    @Column(name = "reference_type", nullable = false)
    private String referenceType;

    /** 외부 도메인 식별자. referenceType 과 함께 조회 키로 쓰인다. */
    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    /** 멱등 키. 동일 키 재요청은 기존 결제를 반환한다(이중과금 방지). DB UNIQUE 로 동시 레이스도 막는다. */
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    /** 결제 금액(원). 0 이하 불가. */
    @Column(nullable = false)
    private long amount;

    /** 통화 코드(예: "KRW"). */
    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    /** PG 식별자(예: "MOCK"). 추상화된 게이트웨이 구현을 가리킨다. */
    @Column(nullable = false)
    private String provider;

    /** PG 가 승인 시 발급하는 거래 식별자. 승인 전에는 null. */
    @Column(name = "provider_tx_id")
    private String providerTxId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 낙관적 락 버전. 결제 상태의 동시 writer(동기 요청 경로 vs PG 웹훅)가 같은 row 를 전이시킬 때
     * lost-update(중복 아웃박스 이벤트 포함)를 막는다. 패자의 커밋은 {@code ObjectOptimisticLockingFailureException}.
     */
    @Version
    @Column(nullable = false)
    private long version;

    public Payment(Long userId, String referenceType, String referenceId,
                   long amount, String currency, String provider, String idempotencyKey) {
        this.userId = requireNonNull(userId, "회원 id");
        this.referenceType = requireText(referenceType, "결제 대상 종류");
        this.referenceId = requireText(referenceId, "결제 대상 id");
        this.amount = requirePositive(amount);
        this.currency = requireText(currency, "통화");
        this.provider = requireText(provider, "결제 수단");
        this.idempotencyKey = requireText(idempotencyKey, "멱등 키");
        this.status = PaymentStatus.PENDING;
    }

    /** PENDING -> APPROVED. PG 거래 식별자를 기록한다. */
    public void approve(String providerTxId) {
        requirePending("승인");
        this.providerTxId = requireText(providerTxId, "PG 거래 id");
        this.status = PaymentStatus.APPROVED;
    }

    /** PENDING -> FAILED. */
    public void fail() {
        requirePending("실패 처리");
        this.status = PaymentStatus.FAILED;
    }

    /** PENDING -> CANCELLED. */
    public void cancel() {
        requirePending("취소");
        this.status = PaymentStatus.CANCELLED;
    }

    private void requirePending(String action) {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "PENDING 상태에서만 " + action + "할 수 있습니다. 현재 상태: " + this.status);
        }
    }

    private static long requirePositive(long amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("결제 금액은 1 이상이어야 합니다: " + amount);
        }
        return amount;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "은(는) 필수입니다.");
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + "은(는) 필수입니다.");
        }
        return value;
    }
}
