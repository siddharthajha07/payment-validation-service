package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

/** StsRsnInf — who rejected the payment, under which code, with optional narrative. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"originator", "reason", "additionalInformation"})
public class StatusReasonInformation {

    @XmlElement(name = "Orgtr")
    private Originator originator;

    @XmlElement(name = "Rsn")
    private Reason reason;

    @XmlElement(name = "AddtlInf")
    private List<String> additionalInformation;

    protected StatusReasonInformation() {
    }

    public StatusReasonInformation(String originatorBic, String reasonCode,
                                   String additionalInformation) {
        this.originator = new Originator(originatorBic);
        this.reason = new Reason(reasonCode);
        this.additionalInformation = additionalInformation == null
                ? List.of() : List.of(additionalInformation);
    }

    public Originator getOriginator() {
        return originator;
    }

    public Reason getReason() {
        return reason;
    }

    public List<String> getAdditionalInformation() {
        return additionalInformation;
    }
}
