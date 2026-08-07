package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/** Orgtr — which party decided the rejection. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"identification"})
public class Originator {

    @XmlElement(name = "Id")
    private OrganisationIdentificationChoice identification;

    protected Originator() {
    }

    public Originator(String bic) {
        this.identification = new OrganisationIdentificationChoice(bic);
    }

    public OrganisationIdentificationChoice getIdentification() {
        return identification;
    }
}
