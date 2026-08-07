package com.rbc.paymentvalidation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * The record of a request already answered, kept so the same request can be answered
 * again identically.
 *
 * <h2>Why the response is stored rather than recomputed</h2>
 * A retry must receive the same answer as the original, and the response carries a digital
 * signature over its exact bytes. Rebuilding it would produce a different creation
 * timestamp and therefore a different signature, so a sender comparing the two would see
 * two different documents for one payment. Storing the response makes a replay genuinely
 * identical.
 *
 * <h2>Why the request is hashed rather than stored</h2>
 * The hash detects the dangerous case: the same idempotency key presented with a
 * <em>different</em> payload, which means the caller has reused a key by mistake and must
 * be told rather than silently handed someone else's result. A hash answers that question
 * without retaining the payload, which carries names and account numbers — the
 * specification's instruction not to store or log sensitive data applies here as much as
 * to the logs.
 */
@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord {

    /** The client-supplied {@code X-Idempotency-Key}. */
    @Id
    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    /** SHA-256 of the request body, hex encoded. */
    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;

    @Lob
    @Column(name = "response_xml", nullable = false, updatable = false)
    private String responseXml;

    @Column(name = "response_status", nullable = false, updatable = false)
    private int responseStatus;

    @Column(name = "transaction_id", length = 35)
    private String transactionId;

    @Column(name = "correlation_id", nullable = false, updatable = false, length = 36)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyRecord() {
        // Required by JPA.
    }

    public IdempotencyRecord(String idempotencyKey, String requestHash, String responseXml,
                             int responseStatus, String transactionId, String correlationId) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.responseXml = responseXml;
        this.responseStatus = responseStatus;
        this.transactionId = transactionId;
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
    }

    /** @return true if the given payload hash matches the one originally recorded. */
    public boolean matchesRequest(String candidateHash) {
        return requestHash.equals(candidateHash);
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public String getResponseXml() {
        return responseXml;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
