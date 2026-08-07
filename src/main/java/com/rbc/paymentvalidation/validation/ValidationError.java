package com.rbc.paymentvalidation.validation;

/**
 * A single reason a payment was rejected.
 *
 * @param reasonCode the ISO code reported to the sender in the pacs.002
 * @param message    what went wrong, in terms of the rule rather than the data. This text
 *                   reaches logs, the audit trail and the response, so it must never
 *                   contain a customer name, an account number or any payload content.
 * @param location   where in the message the fault lies, as an element path such as
 *                   {@code CdtTrfTxInf[0]/DbtrAcct}. Element paths are structural and
 *                   carry no customer data, so they are safe to return and genuinely
 *                   useful to whoever has to repair the message.
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
