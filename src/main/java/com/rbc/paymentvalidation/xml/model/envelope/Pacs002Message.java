package com.rbc.paymentvalidation.xml.model.envelope;

import com.rbc.paymentvalidation.xml.model.header.BusinessApplicationHeader;
import com.rbc.paymentvalidation.xml.model.pacs002.FIToFIPaymentStatusReport;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * The outbound {@code Message} envelope: a business application header paired with a
 * pacs.002 status report.
 *
 * <p>Like the inbound envelope, both children sit in the envelope namespace while their
 * contents resolve to the header and pacs.002 namespaces through the packages their
 * classes live in.
 *
 * <p>This class and {@link Pacs008Message} both declare a root element named
 * {@code Message} in the same namespace, which would be a conflict if they shared a
 * binding context. They do not: the inbound and outbound directions each build their own
 * {@code JAXBContext}, which is also what keeps each context small and quick to create.
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
