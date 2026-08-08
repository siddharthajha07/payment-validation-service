package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * Othr within OrgId — an organisation identifier issued by a private
 * scheme. In the supplied samples this carries the customer reference that this service
 * uses as the natural key for a customer record.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class GenericOrganisationIdentification {

    @XmlElement(name = "Id")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
