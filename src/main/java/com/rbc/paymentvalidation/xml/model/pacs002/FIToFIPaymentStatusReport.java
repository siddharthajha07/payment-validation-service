package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/** FIToFIPmtStsRpt — the pacs.002 document. */
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
