package com.rbc.paymentvalidation.repository;

import com.rbc.paymentvalidation.domain.AuditEvent;
import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * Append-only access to the audit trail.
 *
 * <h2>Why this extends Repository and not JpaRepository</h2>
 * {@code Repository} is a marker interface that declares no methods at all, so this
 * interface has exactly the operations written below and nothing more. Extending
 * {@code JpaRepository} instead would inherit {@code delete}, {@code deleteAll},
 * {@code deleteAllInBatch} and the rest, putting the means to erase the audit trail one
 * autocomplete away from any future contributor.
 *
 * <p>Restricting the interface is a design decision rather than a technical necessity —
 * which is the point. The trail is meant to be append-only, so the type system says so.
 */
public interface AuditEventRepository extends Repository<AuditEvent, Long> {

    AuditEvent save(AuditEvent auditEvent);

    /** The full history of one request, in the order it happened. */
    List<AuditEvent> findByCorrelationIdOrderByOccurredAtAsc(String correlationId);

    /** Everything recorded about one payment. */
    List<AuditEvent> findByPaymentIdOrderByOccurredAtAsc(Long paymentId);
}
