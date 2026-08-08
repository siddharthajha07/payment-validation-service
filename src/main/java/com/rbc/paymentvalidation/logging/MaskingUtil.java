package com.rbc.paymentvalidation.logging;

/**
 * Reduces sensitive values to something safe for a log or the audit trail.
 *
 * Names are never written outside the database column holding them, account numbers appear
 * masked, and payloads are never logged at all, not even at debug level, since debug is what
 * gets switched on during an incident.
 *
 * Four trailing characters is the convention on statements: enough for an operator to confirm
 * the right account, too little to reconstruct it. Short values are masked completely.
 */
public final class MaskingUtil {

    private static final String FULLY_MASKED = "****";
    private static final int VISIBLE_CHARACTERS = 4;

    private MaskingUtil() {
    }

    /**
     * @param accountNumber the account number as written on the message
     * @return the last four characters behind a mask, or a full mask when the value is too
     *         short for that to hide anything
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return FULLY_MASKED;
        }
        String trimmed = accountNumber.trim();
        if (trimmed.length() <= VISIBLE_CHARACTERS) {
            return FULLY_MASKED;
        }
        return FULLY_MASKED + trimmed.substring(trimmed.length() - VISIBLE_CHARACTERS);
    }

    /**
     * @return a marker recording that a value was present without disclosing it. Used where
     *         the fact of a value matters but its content does not — a customer name on a
     *         log line, for instance.
     */
    public static String redact(String value) {
        return value == null || value.isBlank() ? "[absent]" : "[redacted]";
    }

    /**
     * @return a description of a payload by size alone. Every log statement that would
     *         otherwise be tempted to include the message body uses this instead.
     */
    public static String describePayload(byte[] payload) {
        return payload == null ? "[absent]" : "[%d bytes]".formatted(payload.length);
    }
}
