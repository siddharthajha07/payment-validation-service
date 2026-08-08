package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/** DbtrAcct / CdtrAcct — the debtor's or creditor's account. */
@XmlAccessorType(XmlAccessType.FIELD)
public class CashAccount {

    @XmlElement(name = "Id")
    private AccountIdentification identification;

    public AccountIdentification getIdentification() {
        return identification;
    }

    public void setIdentification(AccountIdentification identification) {
        this.identification = identification;
    }

    /**
     * @return the account number, preferring the IBAN when present and falling back to
     *         the generic Othr identifier, or null if neither is present.
     */
    public String accountNumber() {
        if (identification == null) {
            return null;
        }
        if (identification.getIban() != null) {
            return identification.getIban();
        }
        return identification.getOther() == null ? null : identification.getOther().getId();
    }
}
