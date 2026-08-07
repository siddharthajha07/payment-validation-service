package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * {@code StsRsnInf} — who rejected the payment, under which code, with optional narrative.
 *
 * <p>{@code AddtlInf} carries text intended for a person investigating the rejection. It
 * describes the rule that was broken and where in the message, and never the values that
 * broke it: this document is transmitted to another institution and stored at both ends,
 * so customer names and account numbers have no place in it.
 */
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
