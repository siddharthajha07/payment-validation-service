package com.rbc.paymentvalidation.validation;

/**
 * One business rule applied to an incoming payment.
 *
 * To add a rule: implement this, annotate the class @Component and @Order, and it is in.
 * PaymentValidationService receives every implementation as an injected sorted list and has no
 * idea which rules exist, so nothing existing changes.
 *
 * Rules run in @Order sequence and the chain stops at the first failure, so a rule may rely on
 * everything checked before it. Anything after MandatoryFieldsValidator can assume its
 * elements are present; anything after InstitutionValidator can assume the BICs resolve.
 */
public interface PaymentValidator {

    /**
     * @param context the message under validation together with its request metadata
     * @return ValidationResult#valid() when this rule is satisfied, otherwise a
     *         result carrying one or more errors
     */
    ValidationResult validate(ValidationContext context);

    /** @return a human-readable name for logging and the audit trail. */
    default String ruleName() {
        return getClass().getSimpleName();
    }
}
