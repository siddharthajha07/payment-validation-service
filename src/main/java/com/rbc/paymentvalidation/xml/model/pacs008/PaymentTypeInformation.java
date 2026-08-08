package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * PmtTpInf — how the payment should be handled: its service level, local
 * instrument and category purpose.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PaymentTypeInformation {

    @XmlElement(name = "SvcLvl")
    private CodeOrProprietary serviceLevel;

    @XmlElement(name = "LclInstrm")
    private CodeOrProprietary localInstrument;

    @XmlElement(name = "CtgyPurp")
    private CodeOrProprietary categoryPurpose;

    public CodeOrProprietary getServiceLevel() {
        return serviceLevel;
    }

    public void setServiceLevel(CodeOrProprietary serviceLevel) {
        this.serviceLevel = serviceLevel;
    }

    public CodeOrProprietary getLocalInstrument() {
        return localInstrument;
    }

    public void setLocalInstrument(CodeOrProprietary localInstrument) {
        this.localInstrument = localInstrument;
    }

    public CodeOrProprietary getCategoryPurpose() {
        return categoryPurpose;
    }

    public void setCategoryPurpose(CodeOrProprietary categoryPurpose) {
        this.categoryPurpose = categoryPurpose;
    }
}
