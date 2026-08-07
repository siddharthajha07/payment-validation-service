package com.rbc.paymentvalidation.api.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * A transport-level error, returned when no status report can be produced.
 *
 * <h2>Why this is not a pacs.002</h2>
 * A status report must quote the identifiers of the message it reports on. When a payload
 * cannot be parsed, or a required header is absent, those identifiers were never read — so
 * any pacs.002 built in that situation would be inventing the very fields that give it
 * meaning. Returning a plainly different document says honestly that the request never
 * became a payment.
 *
 * <p>Business rejections are the opposite case and behave differently: the message was
 * understood, so it receives a properly signed pacs.002 carrying an ISO reason code.
 *
 * <h2>What it deliberately omits</h2>
 * No stack traces, no exception class names, no internal paths. Those describe the
 * service's construction rather than the caller's mistake, and publishing them hands an
 * attacker a map. The correlation id is included instead: it lets the caller quote one
 * value that leads an operator to the full detail, held where it belongs.
 */
@XmlRootElement(name = "ErrorResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"code", "message", "correlationId", "timestamp", "details"})
public class ErrorResponse {

    /** A stable, machine-readable classification, for example {@code MALFORMED_XML}. */
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
