package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * {@code Orgtr} — which party decided the rejection.
 *
 * <p>Worth stating explicitly rather than leaving implicit: a payment can be rejected by
 * the clearing system, by the creditor agent, or by an intermediary, and the sender's next
 * action differs in each case. The supplied reject sample names the clearing system here,
 * and this service does the same because it is acting in that role.
 *
 * <p>The nesting — {@code Orgtr/Id/OrgId/AnyBIC} — is flattened away by the constructor,
 * so callers supply a BIC and nothing else.
 */
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
