package com.rbc.paymentvalidation.service;

import com.rbc.paymentvalidation.domain.AuditEvent;
import com.rbc.paymentvalidation.domain.AuditEventType;
import com.rbc.paymentvalidation.repository.AuditEventRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records what happened to a request on the append-only audit trail.
 *
 * <h2>Why every event commits in its own transaction</h2>
 * {@code REQUIRES_NEW} suspends whatever transaction is in progress and commits the audit
 * event separately. That is deliberate, and it is the most important decision in this
 * class.
 *
 * <p>Sharing the caller's transaction would mean that when the business transaction rolls
 * back — a constraint violation, an unexpected fault — the audit events roll back with it.
 * The trail would then have no record that the attempt was ever made, and a failed attempt
 * is exactly what an auditor or an operator most needs to see. An audit trail that
 * remembers only successes is not an audit trail.
 *
 * <p>The cost is a separate transaction per event. For a service handling a handful of
 * events per request that is a negligible price for a trail that cannot be erased by the
 * failure it is recording.
 *
 * <h2>What may go in the detail</h2>
 * The detail describes the decision, never the data behind it: which rule rejected the
 * message, not the customer's name, the account number, or any part of the payload. The
 * specification's instruction not to log sensitive customer information applies to the
 * audit trail as much as to the logs.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Records an event that is not yet associated with a stored payment.
     *
     * @param correlationId the request this event belongs to
     * @param eventType     what happened
     * @param detail        why, described in terms of the decision rather than the data
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String correlationId, AuditEventType eventType, String detail) {
        record(correlationId, eventType, null, detail);
    }

    /**
     * Records an event against a stored payment.
     *
     * @param paymentId the payment this event concerns, or {@code null} if none exists yet
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String correlationId, AuditEventType eventType, Long paymentId,
                       String detail) {
        auditEventRepository.save(new AuditEvent(correlationId, eventType, paymentId, detail));
        log.debug("Recorded audit event {} for payment {}", eventType, paymentId);
    }

    /** @return the full history of one request, in the order it happened. */
    @Transactional(readOnly = true)
    public List<AuditEvent> historyOf(String correlationId) {
        return auditEventRepository.findByCorrelationIdOrderByOccurredAtAsc(correlationId);
    }
}
