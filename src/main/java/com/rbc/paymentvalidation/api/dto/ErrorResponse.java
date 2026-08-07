package com.rbc.paymentvalidation.api.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * A transport-level error, for when no status report can be produced.
 *
 * A pacs.002 must quote the identifiers of the message it reports on, and when a payload could
 * not be parsed those were never read, so any pacs.002 built here would be inventing the
 * fields that give it meaning. Returning a plainly different document says honestly that the
 * request never became a payment.
 *
 * No stack traces, exception names or internal paths. The correlation id is included instead,
 * so the caller can quote one value that leads an operator to the detail.
 */
@XmlRootElement(name = "ErrorResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"code", "message", "correlationId", "timestamp", "details"})
public class ErrorResponse {

    /** A stable, machine-readable classification, for example MALFORMED_XML. */
    @XmlElement(name = "Code")
    private String code;

    @XmlElement(name = "Message")
    private String message;

    @XmlElement(name = "CorrelationId")
    private String correlationId;

    @XmlElement(name = "Timestamp")
    private String timestamp;

    /** Specific faults, where naming them helps the caller repair the request. */
    @XmlElementWrapper(name = "Details")
    @XmlElement(name = "Detail")
    private List<String> details;

    protected ErrorResponse() {
        // Required by JAXB.
    }

    public ErrorResponse(String code, String message, String correlationId, String timestamp,
                         List<String> details) {
        this.code = code;
        this.message = message;
        this.correlationId = correlationId;
        this.timestamp = timestamp;
        this.details = details == null || details.isEmpty() ? null : List.copyOf(details);
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public List<String> getDetails() {
        return details;
    }
}
