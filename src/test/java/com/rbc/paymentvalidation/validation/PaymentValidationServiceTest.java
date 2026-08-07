package com.rbc.paymentvalidation.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the behaviour of the chain itself, using stub rules rather than the real ones.
 *
 * Using stubs is deliberate: this class is responsible for how rules are run,
 * not for what any particular rule decides. Testing it against the real rules would couple
 * these tests to every business rule in the system, so a change to one rule would break
 * tests about orchestration.
 */
class PaymentValidationServiceTest {

    private final List<String> executed = new ArrayList<>();

    private PaymentValidator passingRule(String name) {
        return new PaymentValidator() {
            @Override
            public ValidationResult validate(ValidationContext context) {
                executed.add(name);
                return ValidationResult.valid();
            }

            @Override
            public String ruleName() {
                return name;
            }
        };
    }

    private PaymentValidator failingRule(String name, RejectReasonCode reasonCode) {
        return new PaymentValidator() {
            @Override
            public ValidationResult validate(ValidationContext context) {
                executed.add(name);
                return ValidationResult.rejected(reasonCode, name + " rejected this message");
            }

            @Override
            public String ruleName() {
                return name;
            }
        };
    }

    @Test
    @DisplayName("returns a valid result when every rule passes")
    void returnsValidWhenAllRulesPass() {
        PaymentValidationService service = new PaymentValidationService(
                List.of(passingRule("first"), passingRule("second"), passingRule("third")));

        assertThat(service.validate(ValidationFixtures.validContext()).isValid()).isTrue();
        assertThat(executed).containsExactly("first", "second", "third");
    }

    @Test
    @DisplayName("stops at the first rule to fail and does not run the rest")
    void stopsAtFirstFailure() {
        // Rules build on one another: the amount rules assume an amount is present, the
        // account rules assume the institutions resolve. Continuing past a failed
        // prerequisite would report consequences of the first fault as though they were
        // independent problems.
        PaymentValidationService service = new PaymentValidationService(List.of(
                passingRule("first"),
                failingRule("second", RejectReasonCode.AC01),
                passingRule("third")));

        ValidationResult result = service.validate(ValidationFixtures.validContext());

        assertThat(result.isRejected()).isTrue();
        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.AC01);
        assertThat(executed).containsExactly("first", "second");
        assertThat(executed).doesNotContain("third");
    }

    @Test
    @DisplayName("reports the reason code of the rule that failed")
    void reportsTheFailingRulesReasonCode() {
        PaymentValidationService service = new PaymentValidationService(
                List.of(failingRule("duplicate", RejectReasonCode.AM05)));

        assertThat(service.validate(ValidationFixtures.validContext()).primaryError().reasonCode())
                .isEqualTo(RejectReasonCode.AM05);
    }

    @Test
    @DisplayName("runs rules in the order it was given them")
    void runsRulesInGivenOrder() {
        PaymentValidationService service = new PaymentValidationService(
                List.of(passingRule("c"), passingRule("a"), passingRule("b")));

        service.validate(ValidationFixtures.validContext());

        assertThat(service.ruleNames()).containsExactly("c", "a", "b");
        assertThat(executed).containsExactly("c", "a", "b");
    }

    @Test
    @DisplayName("accepts a message when there are no rules at all")
    void acceptsWhenThereAreNoRules() {
        PaymentValidationService service = new PaymentValidationService(List.of());

        assertThat(service.validate(ValidationFixtures.validContext()).isValid()).isTrue();
    }
}
