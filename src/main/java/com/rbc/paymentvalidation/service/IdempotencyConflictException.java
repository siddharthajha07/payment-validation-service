package com.rbc.paymentvalidation.service;

/**
 * Raised when an idempotency key is reused with a different payload.
 *
 * <p>This is the dangerous case, and the reason the request is hashed rather than merely
 * counted. A caller that reuses a key by mistake must be told: replaying the first
 * response would hand them a status report about somebody else's payment, and processing
 * the request afresh would defeat the purpose of the key entirely. Refusing is the only
 * safe answer.
 */
public class IdempotencyConflictException extends RuntimeException {

    private final String idempotencyKey;

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key has already been used with a different payload");
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
