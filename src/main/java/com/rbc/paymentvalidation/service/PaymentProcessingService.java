package com.rbc.paymentvalidation.service;

import com.rbc.paymentvalidation.domain.AuditEventType;
import com.rbc.paymentvalidation.domain.IdempotencyRecord;
import com.rbc.paymentvalidation.domain.Payment;
import com.rbc.paymentvalidation.mapper.Pacs002Factory;
import com.rbc.paymentvalidation.validation.PaymentValidationService;
import com.rbc.paymentvalidation.validation.ValidationContext;
import com.rbc.paymentvalidation.validation.ValidationError;
import com.rbc.paymentvalidation.validation.ValidationResult;
import com.rbc.paymentvalidation.xml.Pacs002Marshaller;
import com.rbc.paymentvalidation.xml.Pacs008Unmarshaller;
import com.rbc.paymentvalidation.xml.SecureXmlParser;
import com.rbc.paymentvalidation.xml.XsdValidator;
import com.rbc.paymentvalidation.xml.model.envelope.Pacs002Message;
import com.rbc.paymentvalidation.xml.model.envelope.Pacs008Message;
import com.rbc.paymentvalidation.xml.signature.XmlSignatureService;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

/**
 * Runs a payment request through the whole pipeline and decides what to send back.
 *
 * <p>This is the one place the sequence is visible end to end. Every step is delegated:
 * this class decides <em>what happens in what order</em>, and nothing else. That is what
 * keeps it readable enough to answer "walk me through a request" by pointing at a single
 * method.
 *
 * <h2>The order is not arbitrary</h2>
 * The idempotency check comes first, before any parsing, because a replay must be
 * recognisable without redoing the work it is replaying — and because a primary-key lookup
 * is the cheapest thing available. Parsing precedes validation because rules cannot be
 * applied to a document that has not been read. Persistence precedes response building
 * because the response reports what was recorded. Storing the idempotency record comes
 * last, once there is a final answer worth replaying.
 *
 * <h2>Why this class is not transactional as a whole</h2>
 * The persistence steps manage their own transactions, and audit events deliberately
 * commit independently. Wrapping everything in one transaction would undo that: a failure
 * late in the pipeline would erase the audit trail describing it.
 */
@Service
public class PaymentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessingService.class);

    private final SecureXmlParser parser;
    private final XsdValidator xsdValidator;
    private final Pacs008Unmarshaller unmarshaller;
    private final PaymentValidationService validationService;
    private final PaymentRecordingService recordingService;
    private final Pacs002Factory pacs002Factory;
    private final Pacs002Marshaller pacs002Marshaller;
    private final XmlSignatureService signatureService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;

    public PaymentProcessingService(SecureXmlParser parser, XsdValidator xsdValidator,
                                    Pacs008Unmarshaller unmarshaller,
                                    PaymentValidationService validationService,
                                    PaymentRecordingService recordingService,
                                    Pacs002Factory pacs002Factory,
                                    Pacs002Marshaller pacs002Marshaller,
                                    XmlSignatureService signatureService,
                                    IdempotencyService idempotencyService,
                                    AuditService auditService) {
        this.parser = parser;
        this.xsdValidator = xsdValidator;
        this.unmarshaller = unmarshaller;
        this.validationService = validationService;
        this.recordingService = recordingService;
        this.pacs002Factory = pacs002Factory;
        this.pacs002Marshaller = pacs002Marshaller;
        this.signatureService = signatureService;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
    }

    /**
     * @param payload         the raw request body
     * @param idempotencyKey  the client's key for this request
     * @param declaredSender  the institution the caller says it is
     * @param correlationId   the identifier tying together everything this request produces
     * @return the status and body to return
     */
    public ProcessingOutcome process(byte[] payload, String idempotencyKey,
                                     String declaredSender, String correlationId) {
        String requestHash = idempotencyService.hash(payload);

        Optional<IdempotencyRecord> previous =
                idempotencyService.findPreviousResponse(idempotencyKey, requestHash);
        if (previous.isPresent()) {
            IdempotencyRecord record = previous.get();
            auditService.record(correlationId, AuditEventType.DUPLICATE_DETECTED,
                    "Replayed the response originally sent for this idempotency key");
            return ProcessingOutcome.replayed(record.getResponseXml(),
                    record.getResponseStatus(), record.getTransactionId());
        }

        auditService.record(correlationId, AuditEventType.MESSAGE_RECEIVED,
                "Request accepted for processing");

        // Parsed once, securely, then reused: handing the bytes to another parser later
        // would reopen the external-entity hole that SecureXmlParser closes.
        Document document = parser.parse(payload);
        xsdValidator.validate(document);
        Pacs008Message request = unmarshaller.unmarshal(document);

        auditService.record(correlationId, AuditEventType.MESSAGE_VALIDATED,
                "Message conforms to the pacs.008 schema");

        ValidationResult validation = validationService.validate(
                new ValidationContext(request, declaredSender, correlationId));

        ProcessingOutcome outcome = validation.isValid()
                ? accept(request, correlationId)
                : reject(request, validation.primaryError(), correlationId);

        idempotencyService.recordResponse(idempotencyKey, requestHash, outcome.responseXml(),
                outcome.httpStatus(), outcome.transactionId(), correlationId);
        return outcome;
    }

    private ProcessingOutcome accept(Pacs008Message request, String correlationId) {
        auditService.record(correlationId, AuditEventType.VALIDATION_PASSED,
                "All business rules passed");

        List<Payment> stored = recordingService.recordAcceptance(request, correlationId);
        String responseXml = signedResponse(pacs002Factory.accept(request), correlationId);

        log.info("Accepted payment message with {} transaction(s)", stored.size());
        return ProcessingOutcome.accepted(responseXml,
                stored.isEmpty() ? null : stored.get(0).getTransactionId());
    }

    private ProcessingOutcome reject(Pacs008Message request, ValidationError error,
                                     String correlationId) {
        Payment stored = recordingService.recordRejection(request, error, correlationId);
        String responseXml = signedResponse(pacs002Factory.reject(request, error), correlationId);

        log.info("Rejected payment message with reason {}", error.reasonCode());
        return ProcessingOutcome.rejected(responseXml,
                stored == null ? null : stored.getTransactionId());
    }

    /**
     * Marshals, signs and serialises a status report.
     *
     * <p>The order is fixed: build the document, sign it, then serialise exactly as it
     * stands. Anything that reformats the document between signing and serialising would
     * invalidate the signature while leaving the XML looking correct.
     */
    private String signedResponse(Pacs002Message response, String correlationId) {
        Document document = pacs002Marshaller.toDocument(response);
        signatureService.sign(document);
        auditService.record(correlationId, AuditEventType.RESPONSE_SIGNED,
                "Status report signed and returned");
        return pacs002Marshaller.toXml(document);
    }
}
