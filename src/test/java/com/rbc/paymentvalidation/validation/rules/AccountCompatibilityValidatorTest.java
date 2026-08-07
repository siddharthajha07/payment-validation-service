package com.rbc.paymentvalidation.validation.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rbc.paymentvalidation.repository.InstitutionRepository;
import com.rbc.paymentvalidation.testsupport.SampleMessages;
import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationError;
import com.rbc.paymentvalidation.validation.ValidationResult;
import com.rbc.paymentvalidation.xml.Pacs008Unmarshaller;
import com.rbc.paymentvalidation.xml.SecureXmlParser;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccountCompatibilityValidatorTest {

    private final InstitutionRepository institutionRepository = mock(InstitutionRepository.class);
    private final AccountCompatibilityValidator validator =
            new AccountCompatibilityValidator(institutionRepository, ValidationFixtures.PROPERTIES);

    @BeforeEach
    void stubInstitutions() {
        when(institutionRepository.findByBicAndActiveTrue("BANKA000"))
                .thenReturn(Optional.of(ValidationFixtures.institutionA()));
        when(institutionRepository.findByBicAndActiveTrue("BANKB000"))
                .thenReturn(Optional.of(ValidationFixtures.institutionB()));
    }

    @Test
    @DisplayName("accepts accounts whose prefixes match their institutions")
    void acceptsMatchingPrefixes() {
        assertThat(validator.validate(ValidationFixtures.validContext()).isValid()).isTrue();
    }

    @Test
    @DisplayName("rejects a debtor account without institution A's FI prefix")
    void rejectsDebtorAccountWithWrongPrefix() {
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith("<Id>FI2003135</Id>", "<Id>XX2003135</Id>")));

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.AC01);
        assertThat(result.primaryError().message())
                .isEqualTo("Account number must begin with FI for institution BANKA000");
    }

    @Test
    @DisplayName("rejects a creditor account without institution B's RI prefix")
    void rejectsCreditorAccountWithWrongPrefix() {
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith("<Id>RI1000331148</Id>",
                        "<Id>FI1000331148</Id>")));

        assertThat(result.errors())
                .extracting(ValidationError::location)
                .contains("CdtTrfTxInf[0]/CdtrAcct");
    }

    @Test
    @DisplayName("rejects the supplied sample, whose transit numbers are five digits")
    void rejectsSuppliedSampleTransitNumbers() {
        // The assessment specifies a three-digit transit number; the supplied sample
        // carries 05605, which is five. This test states that conflict explicitly rather
        // than leaving it as prose in a document. The rule is implemented as specified and
        // the length is configurable, so if the specification is corrected the fix is a
        // configuration change. See ASSUMPTIONS.md.
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                new Pacs008Unmarshaller().unmarshal(
                        new SecureXmlParser(1_048_576).parse(SampleMessages.pacs008Sample()))));

        assertThat(result.isRejected()).isTrue();
        assertThat(result.errors())
                .extracting(ValidationError::reasonCode)
                .contains(RejectReasonCode.RC08);
    }

    @Test
    @DisplayName("rejects a transit number that is not exactly three digits")
    void rejectsTransitNumberOfWrongLength() {
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith("<Id>056</Id>", "<Id>0566</Id>")));

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.RC08);
        assertThat(result.primaryError().message())
                .isEqualTo("Branch transit number must be exactly 3 digits");
    }

    @Test
    @DisplayName("rejects a transit number containing something other than digits")
    void rejectsNonNumericTransitNumber() {
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith("<Id>056</Id>", "<Id>05A</Id>")));

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.RC08);
    }

    @Test
    @DisplayName("rejects a transaction paying an account from itself")
    void rejectsIdenticalDebtorAndCreditorAccounts() {
        // A credit transfer that names one account on both sides moves nothing.
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith("<Id>RI1000331148</Id>", "<Id>FI2003135</Id>")));

        assertThat(result.errors())
                .extracting(ValidationError::message)
                .contains("Debtor and creditor accounts must not be identical");
    }

    @Test
    @DisplayName("does not apply the prefix rule to an institution holding no customer accounts")
    void skipsPrefixCheckForInstitutionWithoutAccounts() {
        // The clearing system is a legitimate counterparty but holds no customer accounts,
        // so the rule is inapplicable rather than violated.
        when(institutionRepository.findByBicAndActiveTrue("BANKB000"))
                .thenReturn(Optional.of(ValidationFixtures.clearingSystem()));

        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith("<Id>RI1000331148</Id>",
                        "<Id>ANYTHING123</Id>")));

        assertThat(result.isValid()).isTrue();
    }
}
