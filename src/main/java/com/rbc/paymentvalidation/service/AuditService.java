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
 * Every event commits in its own transaction. If they shared the caller's, a rollback would
 * erase the record that the attempt was ever made, and a failed attempt is exactly what an
 * auditor wants to see.
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
     * @param paymentId the payment this event concerns, or null if none exists yet
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
