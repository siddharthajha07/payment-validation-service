package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/** OrgId — an organisation identified by its BIC. */
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
