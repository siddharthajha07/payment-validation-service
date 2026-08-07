package com.rbc.paymentvalidation.service;

/**
 * An idempotency key was reused with a different payload.
 *
 * This is why the request is hashed rather than just counted. Replaying would hand the caller
 * a status report about somebody else's payment, and processing afresh would defeat the key
 * entirely. Refusing is the only safe answer.
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
