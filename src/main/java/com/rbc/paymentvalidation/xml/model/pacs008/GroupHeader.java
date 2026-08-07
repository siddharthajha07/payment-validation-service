package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * {@code GrpHdr} — information common to every transaction in the message.
 *
 * <p>{@code numberOfTransactions} and {@code totalInterbankSettlementAmount} are control
 * totals: the sender states how many transactions and what value the message contains,
 * and the receiver checks those claims against what actually arrived. Mismatches indicate
 * truncation or tampering, which is why both are validated rather than merely stored.
 *
 * <p>Date and time fields are {@code String} here. The XSD constrains them to valid
 * {@code xs:date} and {@code xs:dateTime} forms, and conversion to Java date types
 * happens in the mapper, where a failure can be reported as a business error.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class GroupHeader {

    @XmlElement(name = "MsgId")
    private String messageIdentification;

    @XmlElement(name = "CreDtTm")
    private String creationDateTime;

    @XmlElement(name = "NbOfTxs")
    private String numberOfTransactions;

    @XmlElement(name = "TtlIntrBkSttlmAmt")
    private ActiveCurrencyAndAmount totalInterbankSettlementAmount;

    @XmlElement(name = "IntrBkSttlmDt")
    private String interbankSettlementDate;

    @XmlElement(name = "SttlmInf")
    private SettlementInstruction settlementInformation;

    @XmlElement(name = "InstgAgt")
    private BranchAndFinancialInstitutionIdentification instructingAgent;

    public String getMessageIdentification() {
        return messageIdentification;
    }

    public void setMessageIdentification(String messageIdentification) {
        this.messageIdentification = messageIdentification;
    }

    public String getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(String creationDateTime) {
        this.creationDateTime = creationDateTime;
    }

    public String getNumberOfTransactions() {
        return numberOfTransactions;
    }

    public void setNumberOfTransactions(String numberOfTransactions) {
        this.numberOfTransactions = numberOfTransactions;
    }

    public ActiveCurrencyAndAmount getTotalInterbankSettlementAmount() {
        return totalInterbankSettlementAmount;
    }

    public void setTotalInterbankSettlementAmount(ActiveCurrencyAndAmount value) {
        this.totalInterbankSettlementAmount = value;
    }

    public String getInterbankSettlementDate() {
        return interbankSettlementDate;
    }

    public void setInterbankSettlementDate(String interbankSettlementDate) {
        this.interbankSettlementDate = interbankSettlementDate;
    }

    public SettlementInstruction getSettlementInformation() {
        return settlementInformation;
    }

    public void setSettlementInformation(SettlementInstruction settlementInformation) {
        this.settlementInformation = settlementInformation;
    }

    public BranchAndFinancialInstitutionIdentification getInstructingAgent() {
        return instructingAgent;
    }

    public void setInstructingAgent(BranchAndFinancialInstitutionIdentification value) {
        this.instructingAgent = value;
    }
}
