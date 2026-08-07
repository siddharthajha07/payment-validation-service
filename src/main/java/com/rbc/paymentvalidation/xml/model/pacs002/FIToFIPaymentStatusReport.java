package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code FIToFIPmtStsRpt} — the pacs.002 document.
 *
 * <p>Statuses are reported at two levels, matching the supplied samples. The group status
 * reports the fate of the message as a whole; the transaction statuses report individual
 * outcomes. This service decides the whole message at once, so a rejection carries a group
 * status and no transaction entries, while an acceptance carries both.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"groupHeader", "originalGroupInformationAndStatus",
        "transactionInformationAndStatus"})
public class FIToFIPaymentStatusReport {

    @XmlElement(name = "GrpHdr")
    private StatusGroupHeader groupHeader;

    @XmlElement(name = "OrgnlGrpInfAndSts")
    private OriginalGroupHeaderAndStatus originalGroupInformationAndStatus;

    @XmlElement(name = "TxInfAndSts")
    private List<TransactionInfoAndStatus> transactionInformationAndStatus = new ArrayList<>();

    protected FIToFIPaymentStatusReport() {
    }

    public FIToFIPaymentStatusReport(StatusGroupHeader groupHeader,
                                     OriginalGroupHeaderAndStatus originalGroupInformation,
                                     List<TransactionInfoAndStatus> transactionStatuses) {
        this.groupHeader = groupHeader;
        this.originalGroupInformationAndStatus = originalGroupInformation;
        this.transactionInformationAndStatus = new ArrayList<>(transactionStatuses);
    }

    public StatusGroupHeader getGroupHeader() {
        return groupHeader;
    }

    public OriginalGroupHeaderAndStatus getOriginalGroupInformationAndStatus() {
        return originalGroupInformationAndStatus;
    }

    public List<TransactionInfoAndStatus> getTransactionInformationAndStatus() {
        return transactionInformationAndStatus;
    }
}
