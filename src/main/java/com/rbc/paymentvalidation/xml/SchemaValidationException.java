package com.rbc.paymentvalidation.xml;

import java.util.List;

/**
 * Raised when a payload is well-formed XML but does not conform to the message schema —
 * a missing mandatory element, elements out of order, or a value in the wrong lexical
 * form.
 *
 * <p>All schema violations found are retained rather than only the first, so a sender
 * correcting a message learns everything that is wrong in one exchange instead of
 * discovering faults one round trip at a time.
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
