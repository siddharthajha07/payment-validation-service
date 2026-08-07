package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code FIToFICstmrCdtTrf} — the pacs.008 document itself: one group header followed by
 * one or more credit transfer transactions.
 *
 * <p>The transaction list is modelled as a list even though the samples carry a single
 * transaction. A pacs.008 is a batch message by definition, and assuming a single
 * transaction would silently drop the remainder of a valid multi-transaction message.
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
