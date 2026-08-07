package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * {@code FinInstnId} — identifies a financial institution by its BIC.
 *
 * <p>Distinct from the identically named class in the header package: this element
 * belongs to the pacs.008 namespace, that one to the header namespace.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class FinancialInstitutionIdentification {

    @XmlElement(name = "BICFI")
    private String bicfi;

    public String getBicfi() {
        return bicfi;
    }

    public void setBicfi(String bicfi) {
        this.bicfi = bicfi;
    }
}
