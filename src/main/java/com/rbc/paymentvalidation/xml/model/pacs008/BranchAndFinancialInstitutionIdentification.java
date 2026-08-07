package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * An agent in the payment chain — the debtor agent, creditor agent or instructing agent.
 * Combines the institution's BIC with the branch that holds the account.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BranchAndFinancialInstitutionIdentification {

    @XmlElement(name = "FinInstnId")
    private FinancialInstitutionIdentification financialInstitutionIdentification;

    @XmlElement(name = "BrnchId")
    private BranchData branch;

    public FinancialInstitutionIdentification getFinancialInstitutionIdentification() {
        return financialInstitutionIdentification;
    }

    public void setFinancialInstitutionIdentification(FinancialInstitutionIdentification value) {
        this.financialInstitutionIdentification = value;
    }

    public BranchData getBranch() {
        return branch;
    }

    public void setBranch(BranchData branch) {
        this.branch = branch;
    }

    /** @return this agent's BIC, or null if absent. */
    public String bic() {
        return financialInstitutionIdentification == null
                ? null
                : financialInstitutionIdentification.getBicfi();
    }

    /** @return this agent's branch (transit) identifier, or null if absent. */
    public String transitNumber() {
        return branch == null ? null : branch.getId();
    }
}
