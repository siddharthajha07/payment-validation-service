package com.rbc.paymentvalidation.validation.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rbc.paymentvalidation.repository.PaymentRepository;
import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DuplicateValidatorTest {

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final DuplicateValidator validator = new DuplicateValidator(paymentRepository);

    @Test
    @DisplayName("accepts a transaction identifier not seen before")
    void acceptsNewTransaction() {
        when(paymentRepository.existsByTransactionId("B621200494113")).thenReturn(false);

        assertThat(validator.validate(ValidationFixtures.validContext()).isValid()).isTrue();
    }

    @Test
    @DisplayName("rejects a transaction identifier already processed")
    void rejectsAlreadyProcessedTransaction() {
        // Distinct from an idempotent retry: this is a new request carrying a transaction
        // that has been settled before, which is a defect at the sender. Processing it
        // would move the money a second time.
        when(paymentRepository.existsByTransactionId("B621200494113")).thenReturn(true);

        ValidationResult result = validator.validate(ValidationFixtures.validContext());

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.AM05);
        assertThat(result.primaryError().message())
                .isEqualTo("Transaction identifier B621200494113 has already been processed");
    }

    @Test
    @DisplayName("rejects an identifier repeated within a single message")
    void rejectsIdentifierRepeatedWithinMessage() {
        // A repeat inside one batch never reaches the database, so a repository query
        // alone would not see it.
        when(paymentRepository.existsByTransactionId("B621200494113")).thenReturn(false);
        String transaction = ValidationFixtures.duplicatedTransactionMessage();

        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.parse(transaction)));

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.AM05);
        assertThat(result.primaryError().message())
                .isEqualTo("Transaction identifier B621200494113 appears more than once "
                        + "in this message");
    }
}
