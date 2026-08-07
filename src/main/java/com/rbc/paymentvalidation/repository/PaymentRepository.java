package com.rbc.paymentvalidation.repository;

import com.rbc.paymentvalidation.domain.Payment;
import com.rbc.paymentvalidation.domain.PaymentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Transaction lookup and reconciliation.
 *
 * Each finder answers a question someone actually asks: by transaction id for duplicate
 * detection and what happened to this payment, by end-to-end id because that is the reference
 * the originating customer quotes when they call, by settlement date and status for the daily
 * reconciliation view, and by correlation id when troubleshooting one request.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);

    List<Payment> findByEndToEndId(String endToEndId);

    List<Payment> findBySettlementDateAndStatus(LocalDate settlementDate, PaymentStatus status);

    List<Payment> findByCorrelationId(String correlationId);
}
