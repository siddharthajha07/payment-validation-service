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
 * <h2>How immutability is enforced, and where it stops</h2>
 * Three mechanisms, in increasing order of strength:
 * <ol>
 *   <li>The class exposes no setters, so application code has no way to change an event
 *       once it is constructed.</li>
 *   <li>Every column is mapped {@code updatable = false}, so even a mutated managed
 *       instance produces no {@code UPDATE} — Hibernate omits those columns entirely.</li>
 *   <li>{@code AuditEventRepository} declares only {@code save} and finders, so no
 *       deletion or bulk modification operation is reachable from the application.</li>
 * </ol>
 * None of this protects against someone issuing {@code UPDATE audit_event} directly
 * against the database. Genuine tamper-evidence needs controls this application cannot
 * provide alone: revoking {@code UPDATE} and {@code DELETE} from the service's database
 * role, an append-only store, or hash-chaining each row to its predecessor. That is
 * recorded as a documented limit rather than left implied.
 *
 * <h2>Why the payment reference is a plain column</h2>
 * {@code paymentId} is a bare {@code Long}, not a JPA association. Events are recorded
 * from the moment a request arrives, which is before any payment row exists, and a
 * foreign key would either block those early writes or force the trail to start late.
 * The audit trail must be able to record what happened even when nothing else could be
 * written — including the case where processing failed before a payment was ever created.
 *
 * <h2>What may be written to detail</h2>
 * {@code detail} describes the decision, never the data behind it: which rule rejected the
 * message, not the customer's name, account number, or the payload.
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
