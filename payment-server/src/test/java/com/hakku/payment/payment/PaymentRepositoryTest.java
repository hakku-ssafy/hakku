package com.hakku.payment.payment;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hakku.payment.payment.domain.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * idempotency_key 의 DB UNIQUE 제약이 실제로 생성되는지 검증한다(H2).
 * 이 제약이 동시 레이스의 이중 결제 행 생성을 막는 최종 방어선이다.
 */
@SpringBootTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("a second payment with the same idempotency key is rejected by the database")
    void idempotencyKeyIsUnique() {
        paymentRepository.saveAndFlush(
                new Payment(1L, "CART", "cart-1", 1000L, "KRW", "MOCK", "dup-key"));

        assertThrows(DataIntegrityViolationException.class, () ->
                paymentRepository.saveAndFlush(
                        new Payment(2L, "CART", "cart-2", 2000L, "KRW", "MOCK", "dup-key")));
    }
}
