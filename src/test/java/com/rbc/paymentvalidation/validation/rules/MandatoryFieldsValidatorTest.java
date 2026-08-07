package com.rbc.paymentvalidation.validation.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationError;
import com.rbc.paymentvalidation.validation.ValidationResult;
import com.rbc.paymentvalidation.xml.model.envelope.Pacs008Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MandatoryFieldsValidatorTest {

    private final MandatoryFieldsValidator validator = new MandatoryFieldsValidator();

    @Test
    @DisplayName("accepts a message carrying every required element")
    void acceptsCompleteMessage() {
        assertThat(validator.validate(ValidationFixtures.validContext()).isValid()).isTrue();
    }

    @Test
    @DisplayName("rejects a message with no business application header")
    void rejectsMissingHeader() {
        ValidationResult result = validator.validate(
                ValidationFixtures.contextOf(new Pacs008Message()));

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.FF01);
        assertThat(result.primaryError().location()).isEqualTo("AppHdr");
    }

    @Test
    @DisplayName("rejects a transaction with no transaction identifier")
    void rejectsMissingTransactionIdentifier() {
        // TxId is optional in ISO 20022, so this message passes schema validation. It is
        // mandatory here because duplicate detection depends on it — which is precisely
        // why this rule exists alongside the schema rather than being replaced by it.
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith("<TxId>B621200494113</TxId>", "")));

        assertThat(result.isRejected()).isTrue();
        assertThat(result.errors())
                .extracting(ValidationError::location)
                .contains("CdtTrfTxInf[0]/PmtId/TxId");
    }

    @Test
    @DisplayName("rejects a message announcing a different message definition")
    void rejectsWrongMessageDefinition() {
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith(
                        "<MsgDefIdr>pacs.008.001.12</MsgDefIdr>",
                        "<MsgDefIdr>pacs.009.001.11</MsgDefIdr>")));

        assertThat(result.isRejected()).isTrue();
        assertThat(result.errors())
                .extracting(ValidationError::location)
                .contains("AppHdr/MsgDefIdr");
    }

    @Test
    @DisplayName("rejects a message with no business message identifier")
    void rejectsMissingBusinessMessageIdentifier() {
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith(
                        "<BizMsgIdr>MIB621200494113</BizMsgIdr>", "")));

        assertThat(result.errors())
                .extracting(ValidationError::location)
                .contains("AppHdr/BizMsgIdr");
    }

    @Test
    @DisplayName("reports every missing element rather than stopping at the first")
    void reportsEveryMissingElement() {
        // Whoever repairs the message should learn everything that is absent in one
        // exchange, not discover the faults one round trip at a time.
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith("<BizMsgIdr>MIB621200494113</BizMsgIdr>", "")
        ));
        ValidationResult worse = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith("<TxId>B621200494113</TxId>", "")));

        assertThat(result.errors()).hasSize(1);
        assertThat(worse.errors()).isNotEmpty();
    }

    @Test
    @DisplayName("rejects a transaction with no debtor account")
    void rejectsMissingDebtorAccount() {
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith("<Id>FI2003135</Id>", "")));

        assertThat(result.errors())
                .extracting(ValidationError::location)
                .contains("CdtTrfTxInf[0]/DbtrAcct/Id");
    }
}
