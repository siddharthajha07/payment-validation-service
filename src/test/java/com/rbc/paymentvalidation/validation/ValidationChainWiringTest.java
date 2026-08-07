package com.rbc.paymentvalidation.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that the real chain is assembled in the intended order.
 *
 * <p>Ordering is load-bearing here — later rules are written to assume that earlier ones
 * have passed — but it is expressed only as {@code @Order} annotations spread across eight
 * separate files. Nothing in the compiler checks that they are consistent. This test is
 * what turns an ordering mistake into a failing build rather than a
 * {@code NullPointerException} in production.
 */
@SpringBootTest
class ValidationChainWiringTest {

    @Autowired
    private PaymentValidationService validationService;

    @Test
    @DisplayName("assembles every rule in the intended order")
    void assemblesRulesInOrder() {
        assertThat(validationService.ruleNames()).containsExactly(
                "MandatoryFieldsValidator",
                "SenderReceiverValidator",
                "InstitutionValidator",
                "AccountCompatibilityValidator",
                "AmountValidator",
                "ControlTotalsValidator",
                "SettlementDateValidator",
                "DuplicateValidator");
    }

    @Test
    @DisplayName("checks presence before anything that depends on it")
    void checksPresenceFirst() {
        // Every other rule dereferences elements without guarding against absence, which
        // is only safe because this one runs first.
        assertThat(validationService.ruleNames().get(0)).isEqualTo("MandatoryFieldsValidator");
    }

    @Test
    @DisplayName("resolves institutions before applying rules that depend on them")
    void resolvesInstitutionsBeforeAccountRules() {
        // AccountCompatibilityValidator asks an institution what account prefix it uses,
        // a question that only has an answer once the institution is known to exist.
        assertThat(validationService.ruleNames().indexOf("InstitutionValidator"))
                .isLessThan(validationService.ruleNames().indexOf("AccountCompatibilityValidator"));
    }
}
