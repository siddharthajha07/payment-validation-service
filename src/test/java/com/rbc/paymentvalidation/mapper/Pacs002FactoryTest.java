package com.rbc.paymentvalidation.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationError;
import com.rbc.paymentvalidation.xml.model.envelope.Pacs002Message;
import com.rbc.paymentvalidation.xml.model.pacs002.StatusReasonInformation;
import com.rbc.paymentvalidation.xml.model.pacs002.TransactionInfoAndStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Pacs002FactoryTest {

    private final Pacs002Factory factory = new Pacs002Factory(ValidationFixtures.FIXED_CLOCK);

    private Pacs002Message accepted() {
        return factory.accept(ValidationFixtures.validMessage());
    }

    private Pacs002Message rejected() {
        return factory.reject(ValidationFixtures.validMessage(),
                new ValidationError(RejectReasonCode.AC01,
                        "Account number must begin with FI for institution BANKA000",
                        "CdtTrfTxInf[0]/DbtrAcct"));
    }

    @Test
    @DisplayName("answers as the institution the payment was addressed to")
    void reversesTheHeaderParties() {
        // Getting this backwards would produce a response that appears to come from the
        // sender itself. The request went BANKA000 to CBANK0IPS, so the reply is the
        // other way round.
        Pacs002Message response = accepted();

        assertThat(response.getApplicationHeader().senderBic()).isEqualTo("CBANK0IPS");
        assertThat(response.getApplicationHeader().receiverBic()).isEqualTo("BANKA000");
    }

    @Test
    @DisplayName("declares itself a pacs.002")
    void declaresItsOwnMessageDefinition() {
        assertThat(accepted().getApplicationHeader().getMessageDefinitionIdentifier())
                .isEqualTo("pacs.002.001.14");
    }

    @Test
    @DisplayName("quotes the original message so the sender can match the report to it")
    void quotesTheOriginalMessage() {
        var originalStatus = accepted().getStatusReport().getOriginalGroupInformationAndStatus();

        assertThat(originalStatus.getOriginalMessageIdentification())
                .isEqualTo("MIB621200494113");
        assertThat(originalStatus.getOriginalMessageNameIdentification())
                .isEqualTo("pacs.008.001.12");
    }

    @Test
    @DisplayName("reports an acceptance as ACCP with no rejection reason")
    void reportsAcceptance() {
        var originalStatus = accepted().getStatusReport().getOriginalGroupInformationAndStatus();

        assertThat(originalStatus.getGroupStatus()).isEqualTo("ACCP");
        assertThat(originalStatus.getStatusReasonInformation()).isEmpty();
    }

    @Test
    @DisplayName("echoes all three original identifiers for each accepted transaction")
    void echoesEveryOriginalIdentifier() {
        // Not redundancy: the sender reconciles on the transaction id, the originating
        // customer on the end-to-end id, and the instructing party on the instruction id.
        TransactionInfoAndStatus status =
                accepted().getStatusReport().getTransactionInformationAndStatus().get(0);

        assertThat(status.getOriginalInstructionIdentification()).isEqualTo("B621200494113");
        assertThat(status.getOriginalEndToEndIdentification()).isEqualTo("EEB621200494113");
        assertThat(status.getOriginalTransactionIdentification()).isEqualTo("B621200494113");
        assertThat(status.getTransactionStatus()).isEqualTo("ACCP");
    }

    @Test
    @DisplayName("reports a rejection as RJCT with the ISO reason code")
    void reportsRejectionWithIsoCode() {
        var originalStatus = rejected().getStatusReport().getOriginalGroupInformationAndStatus();
        StatusReasonInformation reason = originalStatus.getStatusReasonInformation().get(0);

        assertThat(originalStatus.getGroupStatus()).isEqualTo("RJCT");
        assertThat(reason.getReason().getCode()).isEqualTo("AC01");
    }

    @Test
    @DisplayName("names itself as the party that decided the rejection")
    void namesTheOriginatorOfTheRejection() {
        // The sender's next action differs depending on who rejected the payment, so
        // stating it explicitly matters.
        StatusReasonInformation reason = rejected().getStatusReport()
                .getOriginalGroupInformationAndStatus().getStatusReasonInformation().get(0);

        assertThat(reason.getOriginator().getIdentification()
                .getOrganisationIdentification().getAnyBic()).isEqualTo("CBANK0IPS");
    }

    @Test
    @DisplayName("explains the rejection in terms of the rule and where it failed")
    void explainsTheRejection() {
        StatusReasonInformation reason = rejected().getStatusReport()
                .getOriginalGroupInformationAndStatus().getStatusReasonInformation().get(0);

        assertThat(reason.getAdditionalInformation().get(0))
                .isEqualTo("Account number must begin with FI for institution BANKA000 "
                        + "at CdtTrfTxInf[0]/DbtrAcct");
    }

    @Test
    @DisplayName("reports no per-transaction status when the message was refused as a whole")
    void rejectionCarriesNoTransactionStatuses() {
        assertThat(rejected().getStatusReport().getTransactionInformationAndStatus()).isEmpty();
    }

    @Test
    @DisplayName("gives each status report its own identifier")
    void generatesAUniqueIdentifierPerReport() {
        assertThat(accepted().getApplicationHeader().getBusinessMessageIdentifier())
                .isNotEqualTo(accepted().getApplicationHeader().getBusinessMessageIdentifier());
    }
}
