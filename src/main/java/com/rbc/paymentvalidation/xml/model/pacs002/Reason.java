package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/** Rsn — why the payment was rejected. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"code"})
public class Reason {

    @XmlElement(name = "Cd")
    private String code;

    protected Reason() {
    }

    public Reason(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
