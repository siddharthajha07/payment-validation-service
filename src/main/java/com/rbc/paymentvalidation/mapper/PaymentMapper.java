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
 * Builds a {@link Payment} record from the message that described it.
 *
 * <h2>Why mapping is a separate class</h2>
 * The XML model mirrors the wire format and the entity mirrors the database. Neither
 * should know about the other: if they were the same classes, a change to the ISO schema
 * would force a schema migration, and a change to how we store data would alter what we
 * accept over the wire. This class is the single place those two shapes meet, so a live
 * request to "also persist field X" has one obvious home.
 *
 * <h2>Where date conversion happens</h2>
 * The XML model keeps dates as strings because the XSD has already checked their lexical
 * form. Converting them here rather than during unmarshalling means a conversion failure
 * is an ordinary, reportable outcome instead of an opaque binding exception.
 */
@Component
public class PaymentMapper {

    /**
     * @param header        the business application header
     * @param groupHeader   the group header carrying message-level data
     * @param transaction   the transaction being recorded
     * @param status        the decision reached about it
     * @param correlationId the request that produced it
     * @return an unsaved payment, with accounts still to be attached by the caller
     */
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
     * @return the parsed date, or {@code null} when absent or unparseable. The settlement
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
