package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * Id of a party — in ISO 20022 a choice between an organisation identification
 * and a private (individual) identification. Only the organisation branch is modelled,
 * matching the samples; a private identification would be ignored rather than rejected.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PartyChoice {

    @XmlElement(name = "OrgId")
    private OrganisationIdentification organisationIdentification;

    public OrganisationIdentification getOrganisationIdentification() {
        return organisationIdentification;
    }

    public void setOrganisationIdentification(OrganisationIdentification value) {
        this.organisationIdentification = value;
    }
}
