package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * The recurring ISO choice between a published code and a scheme-specific value.
 *
 * One class serves SvcLvl, LclInstrm, CtgyPurp and ClrSys: they are structurally identical and
 * share a namespace, and the element name comes from the field annotation in the parent.
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
