package com.rbc.paymentvalidation.xml.model.header;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import org.w3c.dom.Element;

/** Sgntr — carries the W3C XML digital signature. */
@XmlAccessorType(XmlAccessType.FIELD)
public class SignatureEnvelope {

    @XmlAnyElement
    private Element signature;

    public Element getSignature() {
        return signature;
    }

    public void setSignature(Element signature) {
        this.signature = signature;
    }
}
