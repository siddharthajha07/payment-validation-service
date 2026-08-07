package com.rbc.paymentvalidation.validation;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Applies the business rules to an incoming payment.
 *
 * <h2>How the rules get here</h2>
 * Spring injects every {@link PaymentValidator} bean as a list, already sorted by each
 * rule's {@code @Order}. This class therefore has no knowledge of which rules exist, and
 * adding one requires no change here at all.
 *
 * <h2>Why the chain stops at the first failure</h2>
 * Two reasons, and both are worth stating plainly.
 *
 * <p>First, correctness: rules build on one another. The amount rules assume an amount is
 * present, the account rules assume the agents resolve to known institutions. Continuing
 * past a failed prerequisite would have later rules examining absent data and reporting
 * faults that are consequences of the first fault rather than independent problems.
 *
 * <p>Second, the shape of the answer: a pacs.002 carries one reason at group level. Since
 * the rules are ordered from most fundamental to most specific, the first failure is the
 * one the sender needs to fix first. A rule may still report several errors of its own —
 * one per offending transaction in a batch, say — and all of them are returned together.
 */
@Service
public class PaymentValidationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentValidationService.class);

    private final List<PaymentValidator> validators;

    public PaymentValidationService(List<PaymentValidator> validators) {
        this.validators = List.copyOf(validators);
        log.info("Validation chain initialised with {} rules: {}",
                validators.size(), validators.stream().map(PaymentValidator::ruleName).toList());
    }

    /**
     * @param context the message under validation
     * @return the result of the first rule to fail, or a valid result if all pass
     */
    public ValidationResult validate(ValidationContext context) {
        for (PaymentValidator validator : validators) {
            ValidationResult result = validator.validate(context);
            if (result.isRejected()) {
                // Logs the rule and the reason code, never the payload or the values that
                // triggered it: an account number in a log line is a data leak regardless
                // of how useful it would have been.
                log.info("Payment rejected by {} with reason {}",
                        validator.ruleName(), result.primaryError().reasonCode());
                return result;
            }
        }
        log.debug("Payment passed all {} validation rules", validators.size());
        return ValidationResult.valid();
    }

    /** @return the rule names in execution order, used by tests and diagnostics. */
    public List<String> ruleNames() {
        return validators.stream().map(PaymentValidator::ruleName).toList();
    }
}
