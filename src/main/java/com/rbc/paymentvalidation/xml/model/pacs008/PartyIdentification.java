package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * A party to the payment — {@code Dbtr}, {@code Cdtr}, {@code UltmtDbtr} or
 * {@code UltmtCdtr}.
 *
 * <p>{@link #customerReference()} flattens {@code Id/OrgId/Othr[0]/Id}, which is the
 * identifier this service treats as the natural key when creating or updating a customer
 * record. It returns {@code null} rather than throwing when any level is missing, because
 * a party without an identifier is a valid message that simply yields no customer record.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PartyIdentification {

    @XmlElement(name = "Nm")
    private String name;

    @XmlElement(name = "Id")
    private PartyChoice identification;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PartyChoice getIdentification() {
        return identification;
    }

    public void setIdentification(PartyChoice identification) {
        this.identification = identification;
    }

    /** @return the first organisation identifier for this party, or {@code null}. */
    public String customerReference() {
        if (identification == null
                || identification.getOrganisationIdentification() == null
                || identification.getOrganisationIdentification().getOther().isEmpty()) {
            return null;
        }
        return identification.getOrganisationIdentification().getOther().get(0).getId();
    }
}
