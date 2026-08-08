package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/** Id of a party — only the organisation branch is used here. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"organisationIdentification"})
public class OrganisationIdentificationChoice {

    @XmlElement(name = "OrgId")
    private OrganisationIdentification organisationIdentification;

    protected OrganisationIdentificationChoice() {
    }

    public OrganisationIdentificationChoice(String bic) {
        this.organisationIdentification = new OrganisationIdentification(bic);
    }

    public OrganisationIdentification getOrganisationIdentification() {
        return organisationIdentification;
    }
}
