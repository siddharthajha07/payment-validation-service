package com.rbc.paymentvalidation.validation;

/**
 * One reason a payment was rejected.
 *
 * The message describes the rule rather than the data, because it reaches logs, the audit
 * trail and the response. The location is an element path such as CdtTrfTxInf[0]/DbtrAcct,
 * which is structural and carries no customer data, so it is safe to return and genuinely
 * useful to whoever has to repair the message.
 */
public record ValidationError(RejectReasonCode reasonCode, String message, String location) {

    public ValidationError(RejectReasonCode reasonCode, String message) {
        this(reasonCode, message, null);
    }

    /** @return the message with its element path appended, when one is known. */
    public String describe() {
        return location == null ? message : "%s at %s".formatted(message, location);
    }
}
