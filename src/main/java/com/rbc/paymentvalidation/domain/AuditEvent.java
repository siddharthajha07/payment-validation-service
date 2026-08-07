package com.rbc.paymentvalidation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One entry on the append-only audit trail.
 *
 * Immutability is enforced three ways: no setters, every column updatable = false, and a
 * repository declaring only save and finders. None of that stops someone running UPDATE
 * audit_event directly; real tamper-evidence needs revoked database grants, an append-only
 * store, or hash-chaining. Recorded as a known limit.
 *
 * paymentId is a plain column, not an association, because events are recorded before any
 * payment row exists and the trail has to work even when nothing else could be written.
 */
@Entity
@Table(name = "audit_event",
        indexes = {
                @Index(name = "idx_audit_correlation_id", columnList = "correlation_id"),
                @Index(name = "idx_audit_payment_id", columnList = "payment_id")
        })
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", nullable = false, updatable = false, length = 36)
    private String correlationId;

    @Column(name = "payment_id", updatable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 40)
    private AuditEventType eventType;

    @Column(name = "detail", updatable = false, length = 500)
    private String detail;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEvent() {
        // Required by JPA.
    }

    public AuditEvent(String correlationId, AuditEventType eventType, Long paymentId,
                      String detail) {
        this.correlationId = correlationId;
        this.eventType = eventType;
        this.paymentId = paymentId;
        this.detail = detail;
        this.occurredAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
