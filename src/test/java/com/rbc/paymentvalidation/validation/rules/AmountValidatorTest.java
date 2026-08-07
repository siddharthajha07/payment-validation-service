package com.rbc.paymentvalidation.validation.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AmountValidatorTest {

    private final AmountValidator validator = new AmountValidator(ValidationFixtures.PROPERTIES);

    private ValidationResult validateWithAmount(String amount) {
        // Targets the transaction amount specifically: the group total is a different
        // element and is checked by ControlTotalsValidator.
        return validator.validate(ValidationFixtures.contextOf(ValidationFixtures.messageWith(
                ">1.02</IntrBkSttlmAmt>", ">" + amount + "</IntrBkSttlmAmt>")));
    }

    @Test
    @DisplayName("accepts a positive amount with two decimal places")
    void acceptsValidAmount() {
        assertThat(validator.validate(ValidationFixtures.validContext()).isValid()).isTrue();
    }

    @Test
    @DisplayName("rejects a zero amount as ZeroAmount")
    void rejectsZeroAmount() {
        assertThat(validateWithAmount("0.00").primaryError().reasonCode())
                .isEqualTo(RejectReasonCode.AM01);
    }

    @Test
    @DisplayName("rejects a negative amount as NotAllowedAmount")
    void rejectsNegativeAmount() {
        // A different fault from zero, and so a different code: the sender's software can
        // distinguish them without parsing English.
        assertThat(validateWithAmount("-5.00").primaryError().reasonCode())
                .isEqualTo(RejectReasonCode.AM02);
    }

    @Test
    @DisplayName("rejects an amount carrying more precision than the currency permits")
    void rejectsExcessivePrecision() {
        // 1.005 cannot be settled in a two-decimal currency without someone rounding it,
        // and rounding another party's money silently is unacceptable.
        ValidationResult result = validateWithAmount("1.005");

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.AM02);
        assertThat(result.primaryError().message())
                .isEqualTo("Settlement amount must have at most 2 decimal places");
    }

    @Test
    @DisplayName("accepts trailing zeros, which add no precision")
    void acceptsTrailingZeros() {
        // 1.020 has a scale of three but carries no more precision than 1.02. Rejecting it
        // would be pedantry rather than protection.
        assertThat(validateWithAmount("1.020").isValid()).isTrue();
    }

    @Test
    @DisplayName("accepts a whole number amount")
    void acceptsWholeNumber() {
        assertThat(validateWithAmount("100").isValid()).isTrue();
    }

    @Test
    @DisplayName("rejects a currency this service does not settle")
    void rejectsUnsupportedCurrency() {
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith("Ccy=\"BBD\"", "Ccy=\"USD\"")));

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.AM03);
        assertThat(result.primaryError().message())
                .isEqualTo("Currency USD is not settled by this service; expected BBD");
    }
}
