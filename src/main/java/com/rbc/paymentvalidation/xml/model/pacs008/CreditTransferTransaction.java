package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * {@code CdtTrfTxInf} — one credit transfer transaction: who is paying whom, how much,
 * through which agents.
 *
 * <p>The distinction between debtor and ultimate debtor matters and is preserved rather
 * than collapsed. {@code Dbtr} is the party whose account is debited; {@code UltmtDbtr}
 * is the party on whose behalf the payment is ultimately made. They are frequently the
 * same, but where they differ the ultimate parties carry the organisation identifier this
 * service uses as the customer reference.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CreditTransferTransaction {

    @XmlElement(name = "PmtId")
    private PaymentIdentification paymentIdentification;

    @XmlElement(name = "PmtTpInf")
    private PaymentTypeInformation paymentTypeInformation;

    @XmlElement(name = "IntrBkSttlmAmt")
    private ActiveCurrencyAndAmount interbankSettlementAmount;

    @XmlElement(name = "AccptncDtTm")
    private String acceptanceDateTime;

    /** Charge bearer, for example {@code SLEV} (following the service level rules). */
    @XmlElement(name = "ChrgBr")
    private String chargeBearer;

    @XmlElement(name = "UltmtDbtr")
    private PartyIdentification ultimateDebtor;

    @XmlElement(name = "Dbtr")
    private PartyIdentification debtor;

    @XmlElement(name = "DbtrAcct")
    private CashAccount debtorAccount;

    @XmlElement(name = "DbtrAgt")
    private BranchAndFinancialInstitutionIdentification debtorAgent;

    @XmlElement(name = "CdtrAgt")
    private BranchAndFinancialInstitutionIdentification creditorAgent;

    @XmlElement(name = "Cdtr")
    private PartyIdentification creditor;

    @XmlElement(name = "CdtrAcct")
    private CashAccount creditorAccount;

    @XmlElement(name = "UltmtCdtr")
    private PartyIdentification ultimateCreditor;

    public PaymentIdentification getPaymentIdentification() {
        return paymentIdentification;
    }

    public void setPaymentIdentification(PaymentIdentification paymentIdentification) {
        this.paymentIdentification = paymentIdentification;
    }

    public PaymentTypeInformation getPaymentTypeInformation() {
        return paymentTypeInformation;
    }

    public void setPaymentTypeInformation(PaymentTypeInformation paymentTypeInformation) {
        this.paymentTypeInformation = paymentTypeInformation;
    }

    public ActiveCurrencyAndAmount getInterbankSettlementAmount() {
        return interbankSettlementAmount;
    }

    public void setInterbankSettlementAmount(ActiveCurrencyAndAmount value) {
        this.interbankSettlementAmount = value;
    }

    public String getAcceptanceDateTime() {
        return acceptanceDateTime;
    }

    public void setAcceptanceDateTime(String acceptanceDateTime) {
        this.acceptanceDateTime = acceptanceDateTime;
    }

    public String getChargeBearer() {
        return chargeBearer;
    }

    public void setChargeBearer(String chargeBearer) {
        this.chargeBearer = chargeBearer;
    }

    public PartyIdentification getUltimateDebtor() {
        return ultimateDebtor;
    }

    public void setUltimateDebtor(PartyIdentification ultimateDebtor) {
        this.ultimateDebtor = ultimateDebtor;
    }

    public PartyIdentification getDebtor() {
        return debtor;
    }

    public void setDebtor(PartyIdentification debtor) {
        this.debtor = debtor;
    }

    public CashAccount getDebtorAccount() {
        return debtorAccount;
    }

    public void setDebtorAccount(CashAccount debtorAccount) {
        this.debtorAccount = debtorAccount;
    }

    public BranchAndFinancialInstitutionIdentification getDebtorAgent() {
        return debtorAgent;
    }

    public void setDebtorAgent(BranchAndFinancialInstitutionIdentification debtorAgent) {
        this.debtorAgent = debtorAgent;
    }

    public BranchAndFinancialInstitutionIdentification getCreditorAgent() {
        return creditorAgent;
    }

    public void setCreditorAgent(BranchAndFinancialInstitutionIdentification creditorAgent) {
        this.creditorAgent = creditorAgent;
    }

    public PartyIdentification getCreditor() {
        return creditor;
    }

    public void setCreditor(PartyIdentification creditor) {
        this.creditor = creditor;
    }

    public CashAccount getCreditorAccount() {
        return creditorAccount;
    }

    public void setCreditorAccount(CashAccount creditorAccount) {
        this.creditorAccount = creditorAccount;
    }

    public PartyIdentification getUltimateCreditor() {
        return ultimateCreditor;
    }

    public void setUltimateCreditor(PartyIdentification ultimateCreditor) {
        this.ultimateCreditor = ultimateCreditor;
    }
}
