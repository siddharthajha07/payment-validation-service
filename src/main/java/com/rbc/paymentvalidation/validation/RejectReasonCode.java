package com.rbc.paymentvalidation.validation;

/**
 * ISO 20022 External Status Reason codes used when rejecting a payment.
 *
 * <p>These are published ISO codes rather than codes invented for this service. That
 * matters: the pacs.002 carrying one is read by another bank's software, which matches on
 * the code to decide what to do next — retry, repair and resend, or refer to the customer.
 * A private code would be meaningless to the receiver and would have to be handled by a
 * human.
 *
 * <p>The description accompanies the code in the audit trail and in the response's
 * additional information, so an operator reading a rejection does not have to look the
 * code up.
 */
public enum RejectReasonCode {

    /** The message is understood but a business-mandatory element is missing. */
    FF01("Invalid file format or missing mandatory business element"),

    /** The instructing and instructed agents are the same institution. */
    AGNT("Incorrect agent"),

    /** A BIC is unknown to this service or the institution is not active. */
    RC01("Bank identifier incorrect"),

    /** An account number is inconsistent with the institution that holds it. */
    AC01("Incorrect account number"),

    /** A branch or transit identifier does not have the expected form. */
    RC08("Invalid clearing system member identifier"),

    /** The settlement amount is zero. */
    AM01("Zero amount"),

    /** The settlement amount is negative or carries more precision than permitted. */
    AM02("Amount not allowed"),

    /** The currency is not supported by this clearing system. */
    AM03("Currency not allowed"),

    /** A payment with this transaction identifier has already been processed. */
    AM05("Duplication"),

    /** The group control total does not equal the sum of the transactions. */
    AM09("Wrong amount"),

    /** The stated number of transactions does not match how many are present. */
    AM18("Invalid number of transactions"),

    /** The settlement date is in the past or too far in the future. */
    DT01("Invalid date");

    private final String description;

    RejectReasonCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
