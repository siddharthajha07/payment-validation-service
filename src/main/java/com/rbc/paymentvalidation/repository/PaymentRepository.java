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
 * <p>Each finder here answers a question someone actually asks:
 * <ul>
 *   <li>by transaction id — duplicate detection, and "what happened to this payment?"</li>
 *   <li>by end-to-end id — the originating customer's own reference, which is what they
 *       quote when they call to ask about a payment</li>
 *   <li>by settlement date and status — the daily reconciliation view</li>
 *   <li>by correlation id — troubleshooting one specific request end to end</li>
 * </ul>
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);

    List<Payment> findByEndToEndId(String endToEndId);

    List<Payment> findBySettlementDateAndStatus(LocalDate settlementDate, PaymentStatus status);

    List<Payment> findByCorrelationId(String correlationId);
}
