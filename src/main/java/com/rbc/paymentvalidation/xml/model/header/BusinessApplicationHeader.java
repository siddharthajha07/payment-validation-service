package com.rbc.paymentvalidation.xml.model.header;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/** AppHdr — the ISO 20022 Business Application Header (head.001.001.03). */
@XmlAccessorType(XmlAccessType.FIELD)
public class BusinessApplicationHeader {

    @XmlElement(name = "Fr")
    private HeaderParty from;

    @XmlElement(name = "To")
    private HeaderParty to;

    /** Business message identifier — the sender's unique reference for this message. */
    @XmlElement(name = "BizMsgIdr")
    private String businessMessageIdentifier;

    /** Message definition identifier, for example pacs.008.001.12. */
    @XmlElement(name = "MsgDefIdr")
    private String messageDefinitionIdentifier;

    /** Business service, for example RTP. */
    @XmlElement(name = "BizSvc")
    private String businessService;

    @XmlElement(name = "CreDt")
    private String creationDate;

    @XmlElement(name = "Sgntr")
    private SignatureEnvelope signature;

    public HeaderParty getFrom() {
        return from;
    }

    public void setFrom(HeaderParty from) {
        this.from = from;
    }

    public HeaderParty getTo() {
        return to;
    }

    public void setTo(HeaderParty to) {
        this.to = to;
    }

    public String getBusinessMessageIdentifier() {
        return businessMessageIdentifier;
    }

    public void setBusinessMessageIdentifier(String businessMessageIdentifier) {
        this.businessMessageIdentifier = businessMessageIdentifier;
    }

    public String getMessageDefinitionIdentifier() {
        return messageDefinitionIdentifier;
    }

    public void setMessageDefinitionIdentifier(String messageDefinitionIdentifier) {
        this.messageDefinitionIdentifier = messageDefinitionIdentifier;
    }

    public String getBusinessService() {
        return businessService;
    }

    public void setBusinessService(String businessService) {
        this.businessService = businessService;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public SignatureEnvelope getSignature() {
        return signature;
    }

    public void setSignature(SignatureEnvelope signature) {
        this.signature = signature;
    }

    /** @return the sender BIC, or null if absent. */
    public String senderBic() {
        return from == null ? null : from.bic();
    }

    /** @return the receiver BIC, or null if absent. */
    public String receiverBic() {
        return to == null ? null : to.bic();
    }
}
