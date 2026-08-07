package com.rbc.paymentvalidation.validation.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationContext;
import com.rbc.paymentvalidation.validation.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SenderReceiverValidatorTest {

    private final SenderReceiverValidator validator = new SenderReceiverValidator();

    @Test
    @DisplayName("accepts a message between two different institutions")
    void acceptsDistinctInstitutions() {
        assertThat(validator.validate(ValidationFixtures.validContext()).isValid()).isTrue();
    }

    @Test
    @DisplayName("rejects a message whose sender and receiver are the same institution")
    void rejectsIdenticalSenderAndReceiver() {
        // Explicitly required by the assessment. An institution cannot clear a payment to
        // itself; such a message is a configuration error at the sender.
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith(
                        "<BICFI>CBANK0IPS</BICFI>", "<BICFI>BANKA000</BICFI>")));

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.AGNT);
        assertThat(result.primaryError().message())
                .isEqualTo("Sender and receiver institutions must not be identical");
    }

    @Test
    @DisplayName("treats institution identifiers case-insensitively when comparing")
    void comparesCaseInsensitively() {
        ValidationContext context = new ValidationContext(
                ValidationFixtures.messageWith("<BICFI>CBANK0IPS</BICFI>",
                        "<BICFI>BANKA000</BICFI>"),
                "banka000", ValidationFixtures.CORRELATION_ID);

        assertThat(validator.validate(context).primaryError().reasonCode())
                .isEqualTo(RejectReasonCode.AGNT);
    }

    @Test
    @DisplayName("rejects a declared sender that disagrees with the message")
    void rejectsMismatchedDeclaredSender() {
        // Either a misconfigured client or an attempt to submit on another institution's
        // behalf. Both warrant telling the caller rather than processing quietly.
        ValidationContext context = new ValidationContext(
                ValidationFixtures.validMessage(), "BANKB000",
                ValidationFixtures.CORRELATION_ID);

        ValidationResult result = validator.validate(context);

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.AGNT);
        assertThat(result.primaryError().location())
                .isEqualTo("X-Sender-Institution and AppHdr/Fr");
    }

    @Test
    @DisplayName("accepts an absent declared sender, leaving the header check to the caller")
    void acceptsAbsentDeclaredSender() {
        ValidationContext context = new ValidationContext(
                ValidationFixtures.validMessage(), null, ValidationFixtures.CORRELATION_ID);

        assertThat(validator.validate(context).isValid()).isTrue();
    }
}
