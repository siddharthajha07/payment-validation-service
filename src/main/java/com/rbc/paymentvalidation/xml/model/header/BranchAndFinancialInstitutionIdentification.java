package com.rbc.paymentvalidation.xml.model.header;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/** FIId — wraps the financial institution identification inside a header party. */
@XmlAccessorType(XmlAccessType.FIELD)
public class BranchAndFinancialInstitutionIdentification {

    @XmlElement(name = "FinInstnId")
    private FinancialInstitutionIdentification financialInstitutionIdentification;

    /** Required by JAXB when reading an inbound message. */
    public BranchAndFinancialInstitutionIdentification() {
    }

    /** Convenience for building an outbound header. */
    public BranchAndFinancialInstitutionIdentification(String bic) {
        this.financialInstitutionIdentification = new FinancialInstitutionIdentification(bic);
    }

    public FinancialInstitutionIdentification getFinancialInstitutionIdentification() {
        return financialInstitutionIdentification;
    }

    public void setFinancialInstitutionIdentification(FinancialInstitutionIdentification value) {
        this.financialInstitutionIdentification = value;
    }
}
