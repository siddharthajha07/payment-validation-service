package com.rbc.paymentvalidation.api;

import com.rbc.paymentvalidation.service.PaymentProcessingService;
import com.rbc.paymentvalidation.service.ProcessingOutcome;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HTTP entry point for submitting a payment.
 *
 * <h2>Why the body is taken as bytes</h2>
 * The request is bound as {@code byte[]} rather than a mapped object. Letting Spring
 * deserialise the XML would mean a parser this service has not hardened reading untrusted
 * input before any of our code runs — precisely the exposure {@code SecureXmlParser}
 * exists to prevent. Taking the raw bytes keeps the hardened parser first in line.
 *
 * <p>It also matters for idempotency: the hash must be over exactly what the client sent,
 * not over a re-serialisation of it.
 *
 * <h2>How thin this class is, deliberately</h2>
 * The controller resolves the correlation id, delegates, and translates the outcome into a
 * response. It contains no business logic at all, which is what allows the pipeline to be
 * tested without HTTP and the HTTP layer to be tested without a database.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    public static final String SENDER_INSTITUTION_HEADER = "X-Sender-Institution";
    public static final String REPLAY_HEADER = "X-Idempotent-Replay";

    private final PaymentProcessingService processingService;

    public PaymentController(PaymentProcessingService processingService) {
        this.processingService = processingService;
    }

    /**
     * Accepts an ISO 20022 pacs.008 and returns a signed pacs.002.
     *
     * @param payload          the pacs.008 message
     * @param idempotencyKey   uniquely identifies this submission, so a retry is recognised
     * @param senderInstitution the BIC the caller claims to be sending as
     * @param correlationId    optional client trace id; one is generated when absent
     * @return the signed status report, with a status reflecting the outcome
     */
    @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> submitPayment(
            @RequestBody byte[] payload,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(SENDER_INSTITUTION_HEADER) String senderInstitution,
            @RequestHeader(name = CORRELATION_ID_HEADER, required = false) String correlationId) {

        String resolvedCorrelationId = resolveCorrelationId(correlationId);
        log.info("Received payment submission");

        ProcessingOutcome outcome = processingService.process(payload, idempotencyKey,
                senderInstitution, resolvedCorrelationId);

        return ResponseEntity.status(outcome.httpStatus())
                .contentType(MediaType.APPLICATION_XML)
                // Echoed so the caller can quote it when reporting a problem, which is what
                // makes one request traceable through logs and the audit trail.
                .header(CORRELATION_ID_HEADER, resolvedCorrelationId)
                .header(REPLAY_HEADER, Boolean.toString(outcome.replay()))
                .body(outcome.responseXml());
    }

    /**
     * @return the caller's correlation id, or a new one. A caller that supplies its own can
     *         match our logs to theirs; one that does not still gets a request that can be
     *         traced from end to end.
     */
    private String resolveCorrelationId(String supplied) {
        if (supplied == null || supplied.isBlank()) {
            return UUID.randomUUID().toString();
        }
        // Bounded to the width of the column that stores it, and to keep a caller from
        // filling the logs with an arbitrarily long value.
        return supplied.length() > 36 ? supplied.substring(0, 36) : supplied;
    }
}
