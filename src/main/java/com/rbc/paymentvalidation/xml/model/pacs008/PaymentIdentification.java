package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * {@code PmtId} — the three identifiers carried by every credit transfer transaction.
 *
 * <p>They serve different purposes and are not interchangeable:
 * <ul>
 *   <li>{@code InstrId} — the instructing party's reference, meaningful only between
 *       two adjacent parties in the chain.</li>
 *   <li>{@code EndToEndId} — assigned by the originating customer and carried unchanged
 *       through the entire chain, which is what makes it useful for reconciliation.</li>
 *   <li>{@code TxId} — the unique reference for this transaction within the clearing
 *       system. This service uses it as the natural key for duplicate detection.</li>
 * </ul>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PaymentIdentification {

    @XmlElement(name = "InstrId")
    private String instructionIdentification;

    @XmlElement(name = "EndToEndId")
    private String endToEndIdentification;

    @XmlElement(name = "TxId")
    private String transactionIdentification;

    public String getInstructionIdentification() {
        return instructionIdentification;
    }

    public void setInstructionIdentification(String instructionIdentification) {
        this.instructionIdentification = instructionIdentification;
    }

    public String getEndToEndIdentification() {
        return endToEndIdentification;
    }

    public void setEndToEndIdentification(String endToEndIdentification) {
        this.endToEndIdentification = endToEndIdentification;
    }

    public String getTransactionIdentification() {
        return transactionIdentification;
    }

    public void setTransactionIdentification(String transactionIdentification) {
        this.transactionIdentification = transactionIdentification;
    }
}
