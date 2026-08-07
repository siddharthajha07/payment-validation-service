package com.rbc.paymentvalidation.xml;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.testsupport.SampleMessages;
import com.rbc.paymentvalidation.xml.model.envelope.Pacs008Message;
import com.rbc.paymentvalidation.xml.model.header.BusinessApplicationHeader;
import com.rbc.paymentvalidation.xml.model.pacs008.CreditTransferTransaction;
import com.rbc.paymentvalidation.xml.model.pacs008.GroupHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that the supplied sample binds correctly to the JAXB model.
 *
 * <p>These assertions guard against the failure mode that matters most in namespaced XML:
 * a mismatched namespace does not raise an error, it silently yields {@code null}. Reading
 * concrete values back out of every part of the message is the only way to prove the
 * binding is actually correct rather than merely quiet.
 */
class Pacs008UnmarshallerTest {

    private final SecureXmlParser parser = new SecureXmlParser(1_048_576);
    private final Pacs008Unmarshaller unmarshaller = new Pacs008Unmarshaller();

    private Pacs008Message message;

    @BeforeEach
    void unmarshalSample() {
        message = unmarshaller.unmarshal(parser.parse(SampleMessages.pacs008Sample()));
    }

    @Test
    @DisplayName("binds the business application header")
    void bindsTheBusinessApplicationHeader() {
        BusinessApplicationHeader header = message.getApplicationHeader();

        assertThat(header).isNotNull();
        assertThat(header.senderBic()).isEqualTo("BANKA000");
        assertThat(header.receiverBic()).isEqualTo("CBANK0IPS");
        assertThat(header.getBusinessMessageIdentifier()).isEqualTo("MIB621200494113");
        assertThat(header.getMessageDefinitionIdentifier()).isEqualTo("pacs.008.001.12");
        assertThat(header.getBusinessService()).isEqualTo("RTP");
        assertThat(header.getCreationDate()).isEqualTo("2026-07-31T11:57:31Z");
    }

    @Test
    @DisplayName("retains the inbound signature as an intact DOM element")
    void retainsTheInboundSignature() {
        assertThat(message.getApplicationHeader().getSignature()).isNotNull();
        assertThat(message.getApplicationHeader().getSignature().getSignature())
                .isNotNull()
                .extracting(element -> element.getLocalName())
                .isEqualTo("Signature");
    }

    @Test
    @DisplayName("binds the group header including its control totals")
    void bindsTheGroupHeader() {
        GroupHeader groupHeader = message.getCreditTransfer().getGroupHeader();

        assertThat(groupHeader.getMessageIdentification()).isEqualTo("MIB621200494113");
        assertThat(groupHeader.getCreationDateTime()).isEqualTo("2026-07-31T07:57:31");
        assertThat(groupHeader.getNumberOfTransactions()).isEqualTo("1");
        assertThat(groupHeader.getInterbankSettlementDate()).isEqualTo("2026-07-31");
        assertThat(groupHeader.getTotalInterbankSettlementAmount().getValue())
                .isEqualByComparingTo("1.02");
        assertThat(groupHeader.getTotalInterbankSettlementAmount().getCurrency())
                .isEqualTo("BBD");
        assertThat(groupHeader.getSettlementInformation().getSettlementMethod())
                .isEqualTo("CLRG");
        assertThat(groupHeader.getSettlementInformation().getClearingSystem().getProprietary())
                .isEqualTo("SENT");
        assertThat(groupHeader.getInstructingAgent().bic()).isEqualTo("BANKA000");
    }

    @Test
    @DisplayName("binds the transaction identifiers")
    void bindsTheTransactionIdentifiers() {
        CreditTransferTransaction transaction = onlyTransaction();

        assertThat(transaction.getPaymentIdentification().getInstructionIdentification())
                .isEqualTo("B621200494113");
        assertThat(transaction.getPaymentIdentification().getEndToEndIdentification())
                .isEqualTo("EEB621200494113");
        assertThat(transaction.getPaymentIdentification().getTransactionIdentification())
                .isEqualTo("B621200494113");
    }

    @Test
    @DisplayName("binds the settlement amount as an exact decimal")
    void bindsTheSettlementAmount() {
        CreditTransferTransaction transaction = onlyTransaction();

        assertThat(transaction.getInterbankSettlementAmount().getValue())
                .isEqualByComparingTo("1.02");
        assertThat(transaction.getInterbankSettlementAmount().getCurrency()).isEqualTo("BBD");
        assertThat(transaction.getChargeBearer()).isEqualTo("SLEV");
        assertThat(transaction.getAcceptanceDateTime()).isEqualTo("2026-07-31T07:57:31");
    }

    @Test
    @DisplayName("binds the payment type information")
    void bindsThePaymentTypeInformation() {
        CreditTransferTransaction transaction = onlyTransaction();

        assertThat(transaction.getPaymentTypeInformation().getServiceLevel().getProprietary())
                .isEqualTo("SENT");
        assertThat(transaction.getPaymentTypeInformation().getLocalInstrument().getCode())
                .isEqualTo("INST");
        assertThat(transaction.getPaymentTypeInformation().getCategoryPurpose().getCode())
                .isEqualTo("SA");
    }

    @Test
    @DisplayName("binds the agents and their branch transit identifiers")
    void bindsTheAgents() {
        CreditTransferTransaction transaction = onlyTransaction();

        assertThat(transaction.getDebtorAgent().bic()).isEqualTo("BANKA000");
        assertThat(transaction.getDebtorAgent().transitNumber()).isEqualTo("05605");
        assertThat(transaction.getCreditorAgent().bic()).isEqualTo("BANKB000");
        assertThat(transaction.getCreditorAgent().transitNumber()).isEqualTo("09606");
    }

    @Test
    @DisplayName("binds the debtor and creditor accounts")
    void bindsTheAccounts() {
        CreditTransferTransaction transaction = onlyTransaction();

        assertThat(transaction.getDebtorAccount().accountNumber()).isEqualTo("2003135");
        assertThat(transaction.getCreditorAccount().accountNumber()).isEqualTo("1000331148");
    }

    @Test
    @DisplayName("flattens the nested organisation identifier into a customer reference")
    void bindsTheCustomerReferences() {
        CreditTransferTransaction transaction = onlyTransaction();

        assertThat(transaction.getDebtor().getName()).isEqualTo("PYRAMID ENT MAN INC");
        assertThat(transaction.getUltimateDebtor().getName()).isEqualTo("PYRAMID ENT MAN INC");
        assertThat(transaction.getUltimateDebtor().customerReference()).isEqualTo("6075857");
        assertThat(transaction.getCreditor().getName()).isEqualTo("test");
        assertThat(transaction.getUltimateCreditor().customerReference()).isEqualTo("6075857");
    }

    @Test
    @DisplayName("returns null rather than failing when an optional identifier is absent")
    void returnsNullForAbsentIdentifier() {
        // The debtor in the sample carries a name but no organisation identifier. A party
        // without one is a valid message that simply yields no customer reference.
        assertThat(onlyTransaction().getDebtor().customerReference()).isNull();
    }

    private CreditTransferTransaction onlyTransaction() {
        assertThat(message.getCreditTransfer().getCreditTransferTransactions()).hasSize(1);
        return message.getCreditTransfer().getCreditTransferTransactions().get(0);
    }
}
