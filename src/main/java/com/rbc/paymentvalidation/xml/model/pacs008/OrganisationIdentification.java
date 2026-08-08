package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/** OrgId — identification of an organisation. */
@XmlAccessorType(XmlAccessType.FIELD)
public class OrganisationIdentification {

    @XmlElement(name = "Othr")
    private List<GenericOrganisationIdentification> other = new ArrayList<>();

    public List<GenericOrganisationIdentification> getOther() {
        return other;
    }

    public void setOther(List<GenericOrganisationIdentification> other) {
        this.other = other;
    }
}
