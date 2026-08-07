package com.rbc.paymentvalidation.xml.model.pacs008;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * {@code BrnchId} — the branch of an agent. Its {@code Id} carries the transit number
 * that the institution/account compatibility rule checks.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class BranchData {

    @XmlElement(name = "Id")
    private String id;

    @XmlElement(name = "Nm")
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
