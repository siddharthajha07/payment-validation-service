package com.rbc.paymentvalidation.xml.model.header;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * {@code AppHdr} — the ISO 20022 Business Application Header (head.001.001.03).
 *
 * <p>The header answers the routing questions — who sent this, to whom, what kind of
 * message is it, and when was it created — independently of the payment itself. That
 * separation is why sender and receiver are validated from the header rather than from
 * the agents named inside the document.
 *
 * <p>{@code creationDate} is deliberately a {@code String}. The XSD already constrains it
 * to a valid {@code xs:dateTime}; converting it to a date type is the mapper's job, which
 * keeps a conversion failure reportable as a business error rather than surfacing as an
 * opaque unmarshalling exception.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BusinessApplicationHeader {

    @XmlElement(name = "Fr")
    private HeaderParty from;

    @XmlElement(name = "To")
    private HeaderParty to;

    /** Business message identifier — the sender's unique reference for this message. */
    @XmlElement(name = "BizMsgIdr")
    private String businessMessageIdentifier;

    /** Message definition identifier, for example {@code pacs.008.001.12}. */
    @XmlElement(name = "MsgDefIdr")
    private String messageDefinitionIdentifier;

    /** Business service, for example {@code RTP}. */
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

    /** @return the sender BIC, or {@code null} if absent. */
    public String senderBic() {
        return from == null ? null : from.bic();
    }

    /** @return the receiver BIC, or {@code null} if absent. */
    public String receiverBic() {
        return to == null ? null : to.bic();
    }
}
