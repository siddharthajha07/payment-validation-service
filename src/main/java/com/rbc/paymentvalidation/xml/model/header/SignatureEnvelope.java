package com.rbc.paymentvalidation.xml.model.header;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyElement;
import org.w3c.dom.Element;

/**
 * {@code Sgntr} — carries the W3C XML digital signature.
 *
 * <p>The signature is held as a raw DOM {@link Element} rather than being modelled as
 * Java classes. A signature covers the exact bytes that were signed, so binding it to
 * objects and writing it back out risks altering whitespace or attribute order and
 * invalidating it. Keeping the original node intact avoids that entirely.
 *
 * <p>Verifying an inbound signature is outside the scope of this assessment, which asks
 * only that the outbound response be signed; the element is retained so its presence can
 * be recorded and so verification could be added without changing this model.
 */
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
