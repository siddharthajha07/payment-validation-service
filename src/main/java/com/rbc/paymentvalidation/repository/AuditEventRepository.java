package com.rbc.paymentvalidation.repository;

import com.rbc.paymentvalidation.domain.AuditEvent;
import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * Append-only access to the audit trail.
 *
 * This extends Repository, a marker interface with no methods, rather than JpaRepository. That
 * means it has exactly the operations written below. Extending JpaRepository would inherit
 * delete, deleteAll and deleteAllInBatch, putting the means to erase the audit trail one
 * autocomplete away from any future contributor.
 *
 * Restricting the interface is a design decision rather than a technical necessity, which is
 * rather the point.
 */
public interface AuditEventRepository extends Repository<AuditEvent, Long> {

    AuditEvent save(AuditEvent auditEvent);

    /** The full history of one request, in the order it happened. */
    List<AuditEvent> findByCorrelationIdOrderByOccurredAtAsc(String correlationId);

    /** Everything recorded about one payment. */
    List<AuditEvent> findByPaymentIdOrderByOccurredAtAsc(Long paymentId);
}
