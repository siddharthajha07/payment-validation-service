package com.rbc.paymentvalidation.xml;

import java.util.List;

/**
 * A well-formed document that does not match the message schema.
 *
 * Every violation is kept rather than just the first, so a sender correcting a message learns
 * everything that is wrong in one exchange instead of one fault per round trip.
 */
public class SchemaValidationException extends XmlProcessingException {

    private final List<String> violations;

    public SchemaValidationException(String message, List<String> violations) {
        super(message);
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
