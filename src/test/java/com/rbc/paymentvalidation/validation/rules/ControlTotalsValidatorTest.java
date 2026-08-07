package com.rbc.paymentvalidation.validation.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ControlTotalsValidatorTest {

    private final ControlTotalsValidator validator = new ControlTotalsValidator();

    @Test
    @DisplayName("accepts a message whose control totals match its contents")
    void acceptsMatchingTotals() {
        assertThat(validator.validate(ValidationFixtures.validContext()).isValid()).isTrue();
    }

    @Test
    @DisplayName("rejects a message declaring more transactions than it carries")
    void rejectsWrongTransactionCount() {
        // The signature that a batch was truncated in transit, or assembled incorrectly.
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith("<NbOfTxs>1</NbOfTxs>",
                        "<NbOfTxs>2</NbOfTxs>")));

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.AM18);
        assertThat(result.primaryError().message())
                .isEqualTo("Message declares 2 transactions but contains 1");
    }

    @Test
    @DisplayName("rejects a group total that does not equal the sum of transactions")
    void rejectsWrongTotalAmount() {
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith(">1.02</TtlIntrBkSttlmAmt>",
                        ">99.99</TtlIntrBkSttlmAmt>")));

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.AM09);
        assertThat(result.primaryError().message())
                .isEqualTo("Declared total 99.99 does not equal the sum of transactions 1.02");
    }

    @Test
    @DisplayName("accepts a total that differs only in scale")
    void acceptsTotalDifferingOnlyInScale() {
        // BigDecimal.equals compares scale as well as value, so 1.020 would not equal 1.02
        // by that method. The comparison uses compareTo, which is what "the total matches"
        // actually means; using equals here would reject valid messages over a zero.
        assertThat(validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith(">1.02</TtlIntrBkSttlmAmt>",
                        ">1.020</TtlIntrBkSttlmAmt>"))).isValid()).isTrue();
    }

    @Test
    @DisplayName("accepts a message that declares no total, which ISO permits")
    void acceptsAbsentTotal() {
        assertThat(validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith(
                        "<TtlIntrBkSttlmAmt Ccy=\"BBD\">1.02</TtlIntrBkSttlmAmt>", "")))
                .isValid()).isTrue();
    }
}
