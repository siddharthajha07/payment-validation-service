package com.rbc.paymentvalidation.validation.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the settlement date window.
 *
 * Every test here runs against a clock fixed at 2026-07-30. Written against the real
 * clock, a test asserting that 2026-07-31 is acceptable would pass today and fail once
 * that date has gone by — a test that expires is worse than no test, because it fails long
 * after the change that supposedly broke it.
 */
class SettlementDateValidatorTest {

    private final SettlementDateValidator validator =
            new SettlementDateValidator(ValidationFixtures.PROPERTIES,
                    ValidationFixtures.FIXED_CLOCK);

    private ValidationResult validateWithDate(String date) {
        return validator.validate(ValidationFixtures.contextOf(ValidationFixtures.messageWith(
                "<IntrBkSttlmDt>2026-07-31</IntrBkSttlmDt>",
                "<IntrBkSttlmDt>" + date + "</IntrBkSttlmDt>")));
    }

    @Test
    @DisplayName("accepts tomorrow, which is inside the settlement window")
    void acceptsDateInsideWindow() {
        assertThat(validator.validate(ValidationFixtures.validContext()).isValid()).isTrue();
    }

    @Test
    @DisplayName("accepts today")
    void acceptsToday() {
        assertThat(validateWithDate("2026-07-30").isValid()).isTrue();
    }

    @Test
    @DisplayName("accepts the last day of the window")
    void acceptsBoundaryOfWindow() {
        // Two days ahead is permitted; this is the boundary, and boundaries are where
        // off-by-one errors live.
        assertThat(validateWithDate("2026-08-01").isValid()).isTrue();
    }

    @Test
    @DisplayName("rejects a settlement date in the past")
    void rejectsPastDate() {
        // Settlement cannot be backdated: the funds movement would have to have happened
        // already. A past date means stale configuration or a message stuck in a queue.
        ValidationResult result = validateWithDate("2026-07-29");

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.DT01);
        assertThat(result.primaryError().message())
                .isEqualTo("Settlement date must not be in the past");
    }

    @Test
    @DisplayName("rejects a settlement date beyond the window")
    void rejectsDateBeyondWindow() {
        ValidationResult result = validateWithDate("2026-08-02");

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.DT01);
        assertThat(result.primaryError().message())
                .isEqualTo("Settlement date must be no more than 2 days ahead");
    }

    @Test
    @DisplayName("accepts a message with no settlement date, which ISO permits")
    void acceptsAbsentDate() {
        assertThat(validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith(
                        "<IntrBkSttlmDt>2026-07-31</IntrBkSttlmDt>", ""))).isValid()).isTrue();
    }
}
