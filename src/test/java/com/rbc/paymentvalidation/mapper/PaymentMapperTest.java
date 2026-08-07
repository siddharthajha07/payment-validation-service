package com.rbc.paymentvalidation.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.domain.Payment;
import com.rbc.paymentvalidation.domain.PaymentStatus;
import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import com.rbc.paymentvalidation.xml.model.envelope.Pacs008Message;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    private Payment map(Pacs008Message message) {
        return mapper.toPayment(message.getApplicationHeader(),
                message.getCreditTransfer().getGroupHeader(),
                message.getCreditTransfer().getCreditTransferTransactions().get(0),
                PaymentStatus.ACCEPTED, "corr-1");
    }

    @Test
    @DisplayName("carries every identifier across from the message")
    void carriesIdentifiers() {
        // Three identifiers with three purposes, all preserved: the instructing party's
        // reference, the customer's own end-to-end reference, and the clearing system's
        // transaction identifier.
        Payment payment = map(ValidationFixtures.validMessage());

        assertThat(payment.getBusinessMessageId()).isEqualTo("MIB621200494113");
        assertThat(payment.getMessageId()).isEqualTo("MIB621200494113");
        assertThat(payment.getInstructionId()).isEqualTo("B621200494113");
        assertThat(payment.getEndToEndId()).isEqualTo("EEB621200494113");
        assertThat(payment.getTransactionId()).isEqualTo("B621200494113");
    }

    @Test
    @DisplayName("carries the amount across without losing precision")
    void carriesAmountExactly() {
        Payment payment = map(ValidationFixtures.validMessage());

        assertThat(payment.getAmount()).isEqualByComparingTo("1.02");
        assertThat(payment.getCurrency()).isEqualTo("BBD");
    }

    @Test
    @DisplayName("converts the settlement date from its string form")
    void convertsSettlementDate() {
        assertThat(map(ValidationFixtures.validMessage()).getSettlementDate())
                .isEqualTo(LocalDate.of(2026, 7, 31));
    }

    @Test
    @DisplayName("carries the agents' identifiers")
    void carriesAgents() {
        Payment payment = map(ValidationFixtures.validMessage());

        assertThat(payment.getDebtorAgentBic()).isEqualTo("BANKA000");
        assertThat(payment.getCreditorAgentBic()).isEqualTo("BANKB000");
    }

    @Test
    @DisplayName("records the decision and the request that produced it")
    void recordsDecisionAndCorrelation() {
        Payment payment = map(ValidationFixtures.validMessage());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.ACCEPTED);
        assertThat(payment.getCorrelationId()).isEqualTo("corr-1");
        assertThat(payment.getReceivedAt()).isNotNull();
    }

    @Test
    @DisplayName("leaves the settlement date absent rather than failing when it is missing")
    void toleratesAbsentSettlementDate() {
        // The date is optional in ISO 20022. Failing here would turn a storable record
        // into a lost one for no benefit.
        Payment payment = map(ValidationFixtures.messageWith(
                "<IntrBkSttlmDt>2026-07-31</IntrBkSttlmDt>", ""));

        assertThat(payment.getSettlementDate()).isNull();
        assertThat(payment.getTransactionId()).isEqualTo("B621200494113");
    }
}
