package com.rbc.paymentvalidation.mapper;

import com.rbc.paymentvalidation.domain.Payment;
import com.rbc.paymentvalidation.domain.PaymentStatus;
import com.rbc.paymentvalidation.xml.model.header.BusinessApplicationHeader;
import com.rbc.paymentvalidation.xml.model.pacs008.CreditTransferTransaction;
import com.rbc.paymentvalidation.xml.model.pacs008.GroupHeader;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;

/**
 * Builds a Payment record from the message that described it.
 *
 * The XML model mirrors the wire format and the entity mirrors the database, and neither should
 * know about the other. This is the single place the two shapes meet, so a request to also
 * persist some field has one obvious home.
 */
@Component
public class PaymentMapper {

    public Payment toPayment(BusinessApplicationHeader header, GroupHeader groupHeader,
                             CreditTransferTransaction transaction, PaymentStatus status,
                             String correlationId) {
        Payment payment = new Payment(
                transaction.getPaymentIdentification().getTransactionIdentification(),
                transaction.getInterbankSettlementAmount().getValue(),
                transaction.getInterbankSettlementAmount().getCurrency(),
                status,
                correlationId);

        payment.setBusinessMessageId(header.getBusinessMessageIdentifier());
        payment.setMessageId(groupHeader.getMessageIdentification());
        payment.setInstructionId(transaction.getPaymentIdentification()
                .getInstructionIdentification());
        payment.setEndToEndId(transaction.getPaymentIdentification()
                .getEndToEndIdentification());
        payment.setSettlementDate(parseDate(groupHeader.getInterbankSettlementDate()));
        payment.setDebtorAgentBic(transaction.getDebtorAgent().bic());
        payment.setCreditorAgentBic(transaction.getCreditorAgent().bic());

        return payment;
    }

    /**
     * @return the parsed date, or null when absent or unparseable. The settlement
     *         date is optional in ISO 20022 and its format has already been validated, so
     *         failing here would turn a stored record into a lost one for no benefit.
     */
    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
