package com.hakku.payment.payment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 결제 애그리거트의 상태 머신(PENDING -> APPROVED/FAILED/CANCELLED)과 생성 불변식 테스트.
 * PENDING 만 비종료 상태이며, 종료 상태에서의 전이는 모두 거부된다.
 */
class PaymentTest {

    private Payment pendingPayment() {
        return new Payment(1L, "CART", "cart-42", 15000L, "KRW", "MOCK", "idem-1");
    }

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("a new payment starts in PENDING with no provider transaction id")
        void newPaymentIsPending() {
            var payment = pendingPayment();

            assertEquals(PaymentStatus.PENDING, payment.getStatus());
            assertNull(payment.getProviderTxId());
        }

        @Test
        @DisplayName("a new payment exposes its idempotency key")
        void exposesIdempotencyKey() {
            var payment = pendingPayment();

            assertEquals("idem-1", payment.getIdempotencyKey());
        }

        @Test
        @DisplayName("amount must be positive")
        void rejectsNonPositiveAmount() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Payment(1L, "CART", "cart-42", 0L, "KRW", "MOCK", "idem-1"));
            assertThrows(IllegalArgumentException.class,
                    () -> new Payment(1L, "CART", "cart-42", -100L, "KRW", "MOCK", "idem-1"));
        }

        @Test
        @DisplayName("reference type and id are required")
        void rejectsBlankReference() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Payment(1L, " ", "cart-42", 15000L, "KRW", "MOCK", "idem-1"));
            assertThrows(IllegalArgumentException.class,
                    () -> new Payment(1L, "CART", "", 15000L, "KRW", "MOCK", "idem-1"));
        }

        @Test
        @DisplayName("currency and provider are required")
        void rejectsBlankCurrencyOrProvider() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Payment(1L, "CART", "cart-42", 15000L, " ", "MOCK", "idem-1"));
            assertThrows(IllegalArgumentException.class,
                    () -> new Payment(1L, "CART", "cart-42", 15000L, "KRW", "", "idem-1"));
        }

        @Test
        @DisplayName("idempotency key is required")
        void rejectsBlankIdempotencyKey() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Payment(1L, "CART", "cart-42", 15000L, "KRW", "MOCK", " "));
        }

        @Test
        @DisplayName("user id is required")
        void rejectsNullUser() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Payment(null, "CART", "cart-42", 15000L, "KRW", "MOCK", "idem-1"));
        }
    }

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("PENDING -> APPROVED records the provider transaction id")
        void approveFromPending() {
            var payment = pendingPayment();

            payment.approve("pg_tx_001");

            assertEquals(PaymentStatus.APPROVED, payment.getStatus());
            assertEquals("pg_tx_001", payment.getProviderTxId());
        }

        @Test
        @DisplayName("a provider transaction id is required to approve")
        void approveRequiresTxId() {
            var payment = pendingPayment();

            assertThrows(IllegalArgumentException.class, () -> payment.approve(" "));
        }

        @Test
        @DisplayName("cannot approve a payment that already left PENDING")
        void approveFromTerminalThrows() {
            var payment = pendingPayment();
            payment.approve("pg_tx_001");

            assertThrows(IllegalStateException.class, () -> payment.approve("pg_tx_002"));
        }

        @Test
        @DisplayName("cannot approve a cancelled payment")
        void approveFromCancelledThrows() {
            var payment = pendingPayment();
            payment.cancel();

            assertThrows(IllegalStateException.class, () -> payment.approve("pg_tx_001"));
        }
    }

    @Nested
    @DisplayName("fail")
    class Fail {

        @Test
        @DisplayName("PENDING -> FAILED")
        void failFromPending() {
            var payment = pendingPayment();

            payment.fail();

            assertEquals(PaymentStatus.FAILED, payment.getStatus());
        }

        @Test
        @DisplayName("cannot fail a payment that already settled")
        void failFromTerminalThrows() {
            var payment = pendingPayment();
            payment.approve("pg_tx_001");

            assertThrows(IllegalStateException.class, payment::fail);
        }

        @Test
        @DisplayName("cannot fail a payment that already failed")
        void failFromFailedThrows() {
            var payment = pendingPayment();
            payment.fail();

            assertThrows(IllegalStateException.class, payment::fail);
        }
    }

    @Nested
    @DisplayName("cancel")
    class Cancel {

        @Test
        @DisplayName("PENDING -> CANCELLED")
        void cancelFromPending() {
            var payment = pendingPayment();

            payment.cancel();

            assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
        }

        @Test
        @DisplayName("cannot cancel a payment that already settled")
        void cancelFromTerminalThrows() {
            var payment = pendingPayment();
            payment.approve("pg_tx_001");

            assertThrows(IllegalStateException.class, payment::cancel);
        }

        @Test
        @DisplayName("cannot cancel a payment that already cancelled")
        void cancelFromCancelledThrows() {
            var payment = pendingPayment();
            payment.cancel();

            assertThrows(IllegalStateException.class, payment::cancel);
        }
    }
}
