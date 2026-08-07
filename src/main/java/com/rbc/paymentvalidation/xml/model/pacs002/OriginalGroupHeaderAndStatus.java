package com.rbc.paymentvalidation.xml.model.pacs002;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

/** OrgnlGrpInfAndSts — identifies the message being reported on and its outcome. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"originalMessageIdentification", "originalMessageNameIdentification",
        "groupStatus", "statusReasonInformation"})
public class OriginalGroupHeaderAndStatus {

    @XmlElement(name = "OrgnlMsgId")
    private String originalMessageIdentification;

    @XmlElement(name = "OrgnlMsgNmId")
    private String originalMessageNameIdentification;

    /** ACCP when accepted, RJCT when rejected. */
    @XmlElement(name = "GrpSts")
    private String groupStatus;

    @XmlElement(name = "StsRsnInf")
    private List<StatusReasonInformation> statusReasonInformation;

    protected OriginalGroupHeaderAndStatus() {
    }

    public OriginalGroupHeaderAndStatus(String originalMessageIdentification,
                                        String originalMessageNameIdentification,
                                        String groupStatus,
                                        StatusReasonInformation reasonInformation) {
        this.originalMessageIdentification = originalMessageIdentification;
        this.originalMessageNameIdentification = originalMessageNameIdentification;
        this.groupStatus = groupStatus;
        this.statusReasonInformation = reasonInformation == null
                ? List.of() : List.of(reasonInformation);
    }

    public String getOriginalMessageIdentification() {
        return originalMessageIdentification;
    }

    public String getOriginalMessageNameIdentification() {
        return originalMessageNameIdentification;
    }

    public String getGroupStatus() {
        return groupStatus;
    }

    public List<StatusReasonInformation> getStatusReasonInformation() {
        return statusReasonInformation;
    }
}
