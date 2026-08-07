package com.rbc.paymentvalidation.logging;

/**
 * Reduces sensitive values to a form safe to write to a log or an audit trail.
 *
 * <h2>What this service will not record</h2>
 * The specification is explicit: no sensitive customer information, and no full XML
 * payloads. This service treats that as an absolute rather than a default. Customer names
 * are never written anywhere outside the database column that holds them. Account numbers
 * appear only masked. Payloads are never logged in whole or in part — not even at debug
 * level, because debug logging is exactly what gets switched on during an incident, which
 * is the worst possible moment to start writing account numbers to disk.
 *
 * <h2>Why the last four digits, and not fewer or none</h2>
 * An operator investigating a payment needs to confirm they are looking at the right
 * account. Masking everything makes two rows indistinguishable and pushes them towards
 * querying the database directly, which is a worse outcome than a partial identifier. Four
 * trailing characters are the familiar convention on statements and receipts: enough to
 * recognise, far too little to reconstruct.
 *
 * <p>Short values are masked entirely. A four-character account revealed by "last four"
 * would not be masked at all, and a rule that silently stops protecting small inputs is a
 * rule nobody can rely on.
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
