package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * Othr — an account identified by a scheme other than IBAN, which is how the
 * domestic accounts in the supplied samples are expressed.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class GenericAccountIdentification {

    @XmlElement(name = "Id")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
