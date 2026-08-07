package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/** {@code FinInstnId} — identifies a financial institution by its BIC. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"bicfi"})
public class FinancialInstitutionIdentification {

    @XmlElement(name = "BICFI")
    private String bicfi;

    protected FinancialInstitutionIdentification() {
    }

    public FinancialInstitutionIdentification(String bicfi) {
        this.bicfi = bicfi;
    }

    public String getBicfi() {
        return bicfi;
    }
}
