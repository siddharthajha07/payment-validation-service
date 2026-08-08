package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * SttlmInf — how the payment settles between the agents. The samples use
 * CLRG (settlement through a clearing system) with a proprietary clearing
 * system identifier.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class SettlementInstruction {

    @XmlElement(name = "SttlmMtd")
    private String settlementMethod;

    @XmlElement(name = "ClrSys")
    private CodeOrProprietary clearingSystem;

    public String getSettlementMethod() {
        return settlementMethod;
    }

    public void setSettlementMethod(String settlementMethod) {
        this.settlementMethod = settlementMethod;
    }

    public CodeOrProprietary getClearingSystem() {
        return clearingSystem;
    }

    public void setClearingSystem(CodeOrProprietary clearingSystem) {
        this.clearingSystem = clearingSystem;
    }
}
