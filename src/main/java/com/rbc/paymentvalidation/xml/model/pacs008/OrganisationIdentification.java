package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code OrgId} — identification of an organisation.
 *
 * <p>{@code Othr} is modelled as a list because ISO 20022 permits repetition, even though
 * the supplied samples carry a single occurrence. Modelling it as a single value would
 * silently discard identifiers in a message that is perfectly valid.
 */
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
