package com.rbc.paymentvalidation.api;

import com.rbc.paymentvalidation.logging.CorrelationId;
import com.rbc.paymentvalidation.logging.MaskingUtil;
import com.rbc.paymentvalidation.service.PaymentProcessingService;
import com.rbc.paymentvalidation.service.ProcessingOutcome;
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
 * The body is bound as byte[] rather than a mapped object, so Spring never puts an unhardened
 * parser in front of untrusted input before our code runs. It also means the idempotency hash
 * is over exactly what the client sent.
 *
 * The correlation id comes from the filter, not a parameter, so requests that never reach this
 * method are traceable too.
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
     * The correlation id is not a parameter here. CorrelationIdFilter has
     * already resolved it and published it to the diagnostic context, which means requests
     * that never reach this method — an unreadable body, a missing header — are traceable
     * too. Those are the requests somebody is most likely to ask about later.
     *
     * @param payload           the pacs.008 message
     * @param idempotencyKey    uniquely identifies this submission, so a retry is recognised
     * @param senderInstitution the BIC the caller claims to be sending as
     * @return the signed status report, with a status reflecting the outcome
     */
    @PostMapping(consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> submitPayment(
            @RequestBody byte[] payload,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(SENDER_INSTITUTION_HEADER) String senderInstitution) {

        String correlationId = CorrelationId.current();
        // Size only. The payload carries names and account numbers and is never logged.
        log.info("Received payment submission {}", MaskingUtil.describePayload(payload));

        ProcessingOutcome outcome = processingService.process(payload, idempotencyKey,
                senderInstitution, correlationId);

        return ResponseEntity.status(outcome.httpStatus())
                .contentType(MediaType.APPLICATION_XML)
                .header(REPLAY_HEADER, Boolean.toString(outcome.replay()))
                .body(outcome.responseXml());
    }
}
