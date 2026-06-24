package com.hakku.payment.payment;

import com.hakku.payment.payment.domain.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** 멱등 키로 기존 결제를 조회한다. 동일 키 재요청의 빠른 경로. */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
