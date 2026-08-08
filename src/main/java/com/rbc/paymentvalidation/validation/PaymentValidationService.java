package com.rbc.paymentvalidation.validation;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Runs the business rules against an incoming payment.
 *
 * Spring injects every PaymentValidator as a list, already sorted by @Order, so this class
 * never needs changing when a rule is added.
 *
 * The chain stops at the first failure for two reasons. Rules build on one another, so
 * continuing past a failed prerequisite would report consequences of the first fault as if
 * they were separate problems. And a pacs.002 carries one reason at group level, so the first
 * failure is the one the sender needs. A single rule may still report several errors of its
 * own, one per offending transaction.
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
