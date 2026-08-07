package com.rbc.paymentvalidation.xml.model.header;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * The {@code Fr} (sender) and {@code To} (receiver) parties of the business header.
 *
 * <p>Convenience accessor {@link #bic()} flattens the four levels of ISO nesting
 * ({@code Fr/FIId/FinInstnId/BICFI}) that callers otherwise have to navigate, returning
 * {@code null} rather than throwing if any level is absent — absence is a validation
 * concern, handled by the validators, not a parsing failure.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class HeaderParty {

    @XmlElement(name = "FIId")
    private BranchAndFinancialInstitutionIdentification financialInstitution;

    /** Required by JAXB when reading an inbound message. */
    public HeaderParty() {
    }

    /** Convenience for building an outbound header. */
    public HeaderParty(String bic) {
        this.financialInstitution = new BranchAndFinancialInstitutionIdentification(bic);
    }

    public BranchAndFinancialInstitutionIdentification getFinancialInstitution() {
        return financialInstitution;
    }

    public void setFinancialInstitution(BranchAndFinancialInstitutionIdentification value) {
        this.financialInstitution = value;
    }

    /** @return the BIC of this party, or {@code null} if any enclosing element is absent. */
    public String bic() {
        if (financialInstitution == null
                || financialInstitution.getFinancialInstitutionIdentification() == null) {
            return null;
        }
        return financialInstitution.getFinancialInstitutionIdentification().getBicfi();
    }
}
