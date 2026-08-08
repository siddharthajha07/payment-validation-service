package com.rbc.paymentvalidation.xml.model.header;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/** FinInstnId — identifies a financial institution by its BIC. */
@XmlAccessorType(XmlAccessType.FIELD)
public class FinancialInstitutionIdentification {

    @XmlElement(name = "BICFI")
    private String bicfi;

    /** Required by JAXB when reading an inbound message. */
    public FinancialInstitutionIdentification() {
    }

    /** Convenience for building an outbound header. */
    public FinancialInstitutionIdentification(String bicfi) {
        this.bicfi = bicfi;
    }

    public String getBicfi() {
        return bicfi;
    }

    public void setBicfi(String bicfi) {
        this.bicfi = bicfi;
    }
}
