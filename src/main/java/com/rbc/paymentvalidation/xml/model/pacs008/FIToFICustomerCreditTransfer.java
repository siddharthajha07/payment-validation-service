package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * FIToFICstmrCdtTrf — the pacs.008 document itself: one group header followed by
 * one or more credit transfer transactions.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class FIToFICustomerCreditTransfer {

    @XmlElement(name = "GrpHdr")
    private GroupHeader groupHeader;

    @XmlElement(name = "CdtTrfTxInf")
    private List<CreditTransferTransaction> creditTransferTransactions = new ArrayList<>();

    public GroupHeader getGroupHeader() {
        return groupHeader;
    }

    public void setGroupHeader(GroupHeader groupHeader) {
        this.groupHeader = groupHeader;
    }

    public List<CreditTransferTransaction> getCreditTransferTransactions() {
        return creditTransferTransactions;
    }

    public void setCreditTransferTransactions(List<CreditTransferTransaction> transactions) {
        this.creditTransferTransactions = transactions;
    }
}
