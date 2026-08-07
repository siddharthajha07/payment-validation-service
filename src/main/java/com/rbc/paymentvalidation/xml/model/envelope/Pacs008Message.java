package com.rbc.paymentvalidation.xml.model.envelope;

import com.rbc.paymentvalidation.xml.model.header.BusinessApplicationHeader;
import com.rbc.paymentvalidation.xml.model.pacs008.FIToFICustomerCreditTransfer;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * The inbound {@code Message} envelope: a business application header paired with a
 * pacs.008 document.
 *
 * <p>Note carefully which namespace each element belongs to. In the samples both children
 * are written with the envelope prefix — {@code <env:AppHdr xmlns="...head.001.001.03">}
 * — so the elements themselves are in the <em>envelope</em> namespace; the default
 * {@code xmlns} declaration they carry applies to their descendants, not to themselves.
 * Both fields therefore inherit the envelope namespace from {@code package-info}, while
 * their contents resolve to the header and pacs.008 namespaces through the packages the
 * child classes live in.
 */
@XmlRootElement(name = "Message")
@XmlAccessorType(XmlAccessType.FIELD)
public class Pacs008Message {

    @XmlElement(name = "AppHdr")
    private BusinessApplicationHeader applicationHeader;

    @XmlElement(name = "FIToFICstmrCdtTrf")
    private FIToFICustomerCreditTransfer creditTransfer;

    public BusinessApplicationHeader getApplicationHeader() {
        return applicationHeader;
    }

    public void setApplicationHeader(BusinessApplicationHeader applicationHeader) {
        this.applicationHeader = applicationHeader;
    }

    public FIToFICustomerCreditTransfer getCreditTransfer() {
        return creditTransfer;
    }

    public void setCreditTransfer(FIToFICustomerCreditTransfer creditTransfer) {
        this.creditTransfer = creditTransfer;
    }
}
