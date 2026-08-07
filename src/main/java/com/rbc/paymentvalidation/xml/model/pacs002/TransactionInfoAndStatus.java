package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * {@code TxInfAndSts} — the outcome of one individual transaction.
 *
 * <p>All three original identifiers are echoed back. That is not redundancy: the sender
 * reconciles on {@code OrgnlTxId}, the originating customer's own system reconciles on
 * {@code OrgnlEndToEndId}, and {@code OrgnlInstrId} is what the immediately instructing
 * party recognises. Returning only one of them would leave somebody in the chain unable to
 * match the report to their record of the payment.
 *
 * <p>{@code OrgnlTxRef}, which the supplied accept sample also carries, is optional in ISO
 * 20022 and is omitted here: it repeats data the sender already holds, and every element
 * modelled is one more thing to keep correct. Recorded in ASSUMPTIONS.md.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"statusIdentification", "originalInstructionIdentification",
        "originalEndToEndIdentification", "originalTransactionIdentification",
        "transactionStatus", "acceptanceDateTime"})
public class TransactionInfoAndStatus {

    @XmlElement(name = "StsId")
    private String statusIdentification;

    @XmlElement(name = "OrgnlInstrId")
    private String originalInstructionIdentification;

    @XmlElement(name = "OrgnlEndToEndId")
    private String originalEndToEndIdentification;

    @XmlElement(name = "OrgnlTxId")
    private String originalTransactionIdentification;

    @XmlElement(name = "TxSts")
    private String transactionStatus;

    @XmlElement(name = "AccptncDtTm")
    private String acceptanceDateTime;

    protected TransactionInfoAndStatus() {
    }

    public TransactionInfoAndStatus(String statusIdentification,
                                    String originalInstructionIdentification,
                                    String originalEndToEndIdentification,
                                    String originalTransactionIdentification,
                                    String transactionStatus, String acceptanceDateTime) {
        this.statusIdentification = statusIdentification;
        this.originalInstructionIdentification = originalInstructionIdentification;
        this.originalEndToEndIdentification = originalEndToEndIdentification;
        this.originalTransactionIdentification = originalTransactionIdentification;
        this.transactionStatus = transactionStatus;
        this.acceptanceDateTime = acceptanceDateTime;
    }

    public String getStatusIdentification() {
        return statusIdentification;
    }

    public String getOriginalInstructionIdentification() {
        return originalInstructionIdentification;
    }

    public String getOriginalEndToEndIdentification() {
        return originalEndToEndIdentification;
    }

    public String getOriginalTransactionIdentification() {
        return originalTransactionIdentification;
    }

    public String getTransactionStatus() {
        return transactionStatus;
    }

    public String getAcceptanceDateTime() {
        return acceptanceDateTime;
    }
}
