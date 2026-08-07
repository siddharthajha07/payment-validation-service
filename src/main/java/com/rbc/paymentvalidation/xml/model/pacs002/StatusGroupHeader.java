package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * {@code GrpHdr} of the status report: who is reporting, when, and to whom.
 *
 * <p>{@code InstdAgt} is the agent being instructed by this report — the institution that
 * sent the original payment and is now being told its outcome.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"messageIdentification", "creationDateTime", "instructedAgent"})
public class StatusGroupHeader {

    @XmlElement(name = "MsgId")
    private String messageIdentification;

    @XmlElement(name = "CreDtTm")
    private String creationDateTime;

    @XmlElement(name = "InstdAgt")
    private BranchAndFinancialInstitutionIdentification instructedAgent;

    protected StatusGroupHeader() {
    }

    public StatusGroupHeader(String messageIdentification, String creationDateTime,
                             String instructedAgentBic) {
        this.messageIdentification = messageIdentification;
        this.creationDateTime = creationDateTime;
        this.instructedAgent =
                new BranchAndFinancialInstitutionIdentification(instructedAgentBic);
    }

    public String getMessageIdentification() {
        return messageIdentification;
    }

    public String getCreationDateTime() {
        return creationDateTime;
    }

    public BranchAndFinancialInstitutionIdentification getInstructedAgent() {
        return instructedAgent;
    }
}
