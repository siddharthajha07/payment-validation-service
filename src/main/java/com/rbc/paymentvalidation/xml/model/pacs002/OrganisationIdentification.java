package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * {@code OrgId} — an organisation identified by its BIC.
 *
 * <p>{@code AnyBIC} rather than {@code BICFI}: the two are different ISO elements. BICFI
 * identifies a financial institution acting as an agent, while AnyBIC identifies any
 * organisation, which is the correct element when naming who originated a status decision.
 * The supplied reject sample uses AnyBIC here, and this model follows it.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"anyBic"})
public class OrganisationIdentification {

    @XmlElement(name = "AnyBIC")
    private String anyBic;

    protected OrganisationIdentification() {
    }

    public OrganisationIdentification(String anyBic) {
        this.anyBic = anyBic;
    }

    public String getAnyBic() {
        return anyBic;
    }
}
