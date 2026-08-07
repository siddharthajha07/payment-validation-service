package com.rbc.paymentvalidation.domain;

/**
 * The outcome of processing a payment instruction.
 *
 * <p>These map directly onto the {@code GrpSts} values of the pacs.002 response, which is
 * why there is no {@code PENDING} or {@code IN_PROGRESS}: this service decides
 * synchronously and answers in the same exchange. A status that could never appear in a
 * response would be a status the sender can never be told about.
 */
public enum PaymentStatus {

    /** Accepted for further processing — reported as {@code ACCP}. */
    ACCEPTED,

    /** Rejected on a business rule — reported as {@code RJCT} with a reason code. */
    REJECTED
}
