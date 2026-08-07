package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * The recurring ISO 20022 "code or proprietary" choice: either an externally published
 * code ({@code Cd}) or a scheme-specific value ({@code Prtry}).
 *
 * <p>One class serves {@code SvcLvl}, {@code LclInstrm}, {@code CtgyPurp} and
 * {@code ClrSys}, which are structurally identical and share the pacs.008 namespace. The
 * element name is supplied by the {@code @XmlElement} annotation on the containing field,
 * so the same binding is reusable across all four without ambiguity.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CodeOrProprietary {

    @XmlElement(name = "Cd")
    private String code;

    @XmlElement(name = "Prtry")
    private String proprietary;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getProprietary() {
        return proprietary;
    }

    public void setProprietary(String proprietary) {
        this.proprietary = proprietary;
    }

    /** @return the code if present, otherwise the proprietary value, otherwise null. */
    public String value() {
        return code != null ? code : proprietary;
    }
}
