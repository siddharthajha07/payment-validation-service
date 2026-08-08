package com.rbc.paymentvalidation.domain;

/**
 * The outcome of processing a payment.
 *
 * These map onto the GrpSts values of the pacs.002, which is why there is no PENDING: this
 * service decides synchronously and answers in the same exchange, so a status that could never
 * appear in a response would be one the sender can never be told about.
 */
public enum PaymentStatus {

    /** Accepted for further processing — reported as ACCP. */
    ACCEPTED,

    /** Rejected on a business rule — reported as RJCT with a reason code. */
    REJECTED
}
