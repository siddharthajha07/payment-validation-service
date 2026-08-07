package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/** An agent referenced by the status report, such as {@code InstdAgt}. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"financialInstitutionIdentification"})
public class BranchAndFinancialInstitutionIdentification {

    @XmlElement(name = "FinInstnId")
    private FinancialInstitutionIdentification financialInstitutionIdentification;

    protected BranchAndFinancialInstitutionIdentification() {
    }

    public BranchAndFinancialInstitutionIdentification(String bic) {
        this.financialInstitutionIdentification = new FinancialInstitutionIdentification(bic);
    }

    public FinancialInstitutionIdentification getFinancialInstitutionIdentification() {
        return financialInstitutionIdentification;
    }
}
