package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * {@code Rsn} — why the payment was rejected.
 *
 * <p>Always populated with the published ISO External Status Reason code rather than the
 * proprietary alternative. The receiving bank's software matches on this code to decide
 * whether to repair and resend, retry, or refer to its customer; a private code would mean
 * nothing to it and would have to be handled by a person.
 */
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
