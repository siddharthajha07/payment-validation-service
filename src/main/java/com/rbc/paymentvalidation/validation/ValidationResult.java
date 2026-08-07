package com.rbc.paymentvalidation.validation;

import java.util.List;

/**
 * The outcome of applying a validation rule, or of the whole chain.
 *
 * <p>A result is valid when it holds no errors. A single rule may report several errors at
 * once — an amount validator checking every transaction in a batch, for example — while
 * the chain as a whole reports the errors of the first rule that failed.
 *
 * @param errors every fault this rule found, in the order found
 */
public record ValidationResult(List<ValidationError> errors) {

    public ValidationResult {
        errors = List.copyOf(errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public boolean isRejected() {
        return !errors.isEmpty();
    }

    /**
     * @return the error whose reason code is reported to the sender. A pacs.002 carries
     *         one primary reason at group level, and rules run in order of significance,
     *         so the first error found is the most fundamental one.
     */
    public ValidationError primaryError() {
        if (errors.isEmpty()) {
            throw new IllegalStateException("A valid result has no primary error");
        }
        return errors.get(0);
    }

    public static ValidationResult valid() {
        return new ValidationResult(List.of());
    }

    public static ValidationResult rejected(RejectReasonCode reasonCode, String message) {
        return new ValidationResult(List.of(new ValidationError(reasonCode, message)));
    }

    public static ValidationResult rejected(RejectReasonCode reasonCode, String message,
                                            String location) {
        return new ValidationResult(List.of(new ValidationError(reasonCode, message, location)));
    }

    public static ValidationResult rejected(List<ValidationError> errors) {
        return new ValidationResult(errors);
    }
}
