package com.rbc.paymentvalidation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A request already answered, kept so the same request can be answered identically.
 *
 * The response is stored rather than recomputed because it carries a signature over its exact
 * bytes; rebuilding it would produce a different timestamp and therefore a different
 * signature, so a client comparing the two would see two documents for one payment.
 *
 * The request is hashed rather than stored. That detects the dangerous case, the same key with
 * a different payload, without retaining a body full of names and account numbers.
 */
@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord {

    /** The client-supplied X-Idempotency-Key. */
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
