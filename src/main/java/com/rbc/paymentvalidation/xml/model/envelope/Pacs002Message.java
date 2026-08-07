package com.rbc.paymentvalidation.xml.model.envelope;

import com.rbc.paymentvalidation.xml.model.header.BusinessApplicationHeader;
import com.rbc.paymentvalidation.xml.model.pacs002.FIToFIPaymentStatusReport;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * The outbound Message envelope: a business application header paired with a
 * pacs.002 status report.
 */
@XmlRootElement(name = "Message")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"applicationHeader", "statusReport"})
public class Pacs002Message {

    @XmlElement(name = "AppHdr")
    private BusinessApplicationHeader applicationHeader;

    @XmlElement(name = "FIToFIPmtStsRpt")
    private FIToFIPaymentStatusReport statusReport;

    protected Pacs002Message() {
    }

    public Pacs002Message(BusinessApplicationHeader applicationHeader,
                          FIToFIPaymentStatusReport statusReport) {
        this.applicationHeader = applicationHeader;
        this.statusReport = statusReport;
    }

    public BusinessApplicationHeader getApplicationHeader() {
        return applicationHeader;
    }

    public FIToFIPaymentStatusReport getStatusReport() {
        return statusReport;
    }
}
