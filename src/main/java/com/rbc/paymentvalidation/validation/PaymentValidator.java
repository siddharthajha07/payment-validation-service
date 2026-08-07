package com.rbc.paymentvalidation.validation;

/**
 * One business rule applied to an incoming payment.
 *
 * <h2>How a rule joins the chain</h2>
 * Implement this interface, annotate the class {@code @Component} and {@code @Order}, and
 * it is in. {@link PaymentValidationService} receives every implementation as an injected
 * list, already sorted, and has no knowledge of which rules exist. Adding, removing or
 * reordering a rule therefore changes no existing class — the chain is open to extension
 * and closed to modification.
 *
 * <h2>What an implementation may assume</h2>
 * Rules run in {@code @Order} sequence and the chain stops at the first failure, so a rule
 * may rely on everything checked before it. In particular, anything after
 * {@code MandatoryFieldsValidator} may assume the elements it needs are present, and
 * anything after {@code InstitutionValidator} may assume the agents' BICs resolve to
 * known, active institutions. That is why the ordering is explicit and documented rather
 * than incidental.
 */
public interface PaymentValidator {

    /**
     * @param context the message under validation together with its request metadata
     * @return {@link ValidationResult#valid()} when this rule is satisfied, otherwise a
     *         result carrying one or more errors
     */
    ValidationResult validate(ValidationContext context);

    /** @return a human-readable name for logging and the audit trail. */
    default String ruleName() {
        return getClass().getSimpleName();
    }
}
