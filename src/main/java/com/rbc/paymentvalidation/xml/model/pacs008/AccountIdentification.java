package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * {@code Id} of a cash account — in ISO 20022 a choice between an IBAN and any other
 * scheme. Both are modelled; the validators decide which is acceptable.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class AccountIdentification {

    @XmlElement(name = "IBAN")
    private String iban;

    @XmlElement(name = "Othr")
    private GenericAccountIdentification other;

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public GenericAccountIdentification getOther() {
        return other;
    }

    public void setOther(GenericAccountIdentification other) {
        this.other = other;
    }
}
