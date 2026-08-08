package com.rbc.paymentvalidation.domain;

/**
 * The kinds of event recorded on the audit trail.
 *
 * Built around the questions someone actually asks afterwards: did the message arrive, was it
 * understood, why was it rejected, what did it change about our customer records, and what did
 * we send back. Every event carries the correlation id of the request that produced it.
 */
public enum AuditEventType {

    /** A request arrived and passed header checks. */
    MESSAGE_RECEIVED,

    /** The payload parsed and conformed to the schema. */
    MESSAGE_VALIDATED,

    /** Every business rule passed. */
    VALIDATION_PASSED,

    /** A business rule rejected the message; the detail records which. */
    VALIDATION_FAILED,

    /** The request repeated one already processed; the stored response was replayed. */
    DUPLICATE_DETECTED,

    CUSTOMER_CREATED,
    CUSTOMER_UPDATED,
    ACCOUNT_CREATED,
    ACCOUNT_UPDATED,

    /** The payment was written to the transaction store. */
    PAYMENT_RECORDED,

    /** A pacs.002 was generated and signed. */
    RESPONSE_SIGNED
}
