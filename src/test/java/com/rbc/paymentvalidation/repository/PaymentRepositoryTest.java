package com.rbc.paymentvalidation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rbc.paymentvalidation.domain.Payment;
import com.rbc.paymentvalidation.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment acceptedPayment(String transactionId) {
        return new Payment(transactionId, new BigDecimal("1.02"), "BBD",
                PaymentStatus.ACCEPTED, "corr-1");
    }

    @Test
    @DisplayName("finds a payment by its transaction identifier")
    void findsPaymentByTransactionId() {
        paymentRepository.save(acceptedPayment("B621200494113"));

        assertThat(paymentRepository.findByTransactionId("B621200494113")).isPresent();
        assertThat(paymentRepository.existsByTransactionId("B621200494113")).isTrue();
    }

    @Test
    @DisplayName("refuses a second payment carrying the same transaction identifier")
    void refusesDuplicateTransactionId() {
        // Duplicate detection is ultimately guaranteed here, in the database. Two
        // concurrent requests can both pass an application-level existence check before
        // either inserts; this constraint is what stops the second from succeeding.
        paymentRepository.saveAndFlush(acceptedPayment("B621200494113"));

        assertThatThrownBy(() ->
                paymentRepository.saveAndFlush(acceptedPayment("B621200494113")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("stores rejected payments as well as accepted ones")
    void storesRejectedPayments() {
        // Rejections are what an operator most often needs to look up: a sender asking
        // why a payment did not arrive is asking about a rejected row.
        Payment payment = acceptedPayment("B621200494113");
        payment.recordRejection("AC01", "Debtor account prefix does not match institution");

        Payment saved = paymentRepository.save(payment);

        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(saved.getReasonCode()).isEqualTo("AC01");
        assertThat(saved.getReasonDescription())
                .isEqualTo("Debtor account prefix does not match institution");
    }

    @Test
    @DisplayName("preserves the amount exactly rather than as a binary approximation")
    void preservesAmountExactly() {
        // 1.02 has no exact binary representation. Stored as a double it would come back
        // as something very slightly different, which is unacceptable for money.
        paymentRepository.saveAndFlush(acceptedPayment("B621200494113"));

        assertThat(paymentRepository.findByTransactionId("B621200494113"))
                .get()
                .extracting(Payment::getAmount)
                .satisfies(amount ->
                        assertThat((BigDecimal) amount).isEqualByComparingTo("1.02"));
    }

    @Test
    @DisplayName("finds payments by the originating customer's own reference")
    void findsPaymentsByEndToEndId() {
        // The end-to-end identifier is what a customer quotes when they call to ask about
        // a payment, because it is the reference they themselves assigned.
        Payment payment = acceptedPayment("B621200494113");
        payment.setEndToEndId("EEB621200494113");
        paymentRepository.save(payment);

        assertThat(paymentRepository.findByEndToEndId("EEB621200494113")).hasSize(1);
    }

    @Test
    @DisplayName("supports the daily reconciliation view")
    void supportsReconciliationBySettlementDateAndStatus() {
        Payment settledToday = acceptedPayment("TX-1");
        settledToday.setSettlementDate(LocalDate.of(2026, 7, 31));
        Payment settledTomorrow = acceptedPayment("TX-2");
        settledTomorrow.setSettlementDate(LocalDate.of(2026, 8, 1));
        paymentRepository.saveAll(java.util.List.of(settledToday, settledTomorrow));

        assertThat(paymentRepository.findBySettlementDateAndStatus(
                LocalDate.of(2026, 7, 31), PaymentStatus.ACCEPTED)).hasSize(1);
    }

    @Test
    @DisplayName("finds every payment belonging to one request")
    void findsPaymentsByCorrelationId() {
        paymentRepository.save(acceptedPayment("TX-1"));
        paymentRepository.save(acceptedPayment("TX-2"));

        assertThat(paymentRepository.findByCorrelationId("corr-1")).hasSize(2);
    }
}
