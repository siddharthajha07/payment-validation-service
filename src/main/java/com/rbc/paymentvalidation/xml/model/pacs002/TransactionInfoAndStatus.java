package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/** TxInfAndSts — the outcome of one individual transaction. */
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
