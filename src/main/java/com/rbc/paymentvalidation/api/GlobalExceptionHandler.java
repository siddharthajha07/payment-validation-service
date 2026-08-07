package com.rbc.paymentvalidation.api;

import com.rbc.paymentvalidation.api.dto.ErrorResponse;
import com.rbc.paymentvalidation.logging.CorrelationId;
import com.rbc.paymentvalidation.service.IdempotencyConflictException;
import com.rbc.paymentvalidation.xml.SchemaValidationException;
import com.rbc.paymentvalidation.xml.XmlProcessingException;
import com.rbc.paymentvalidation.xml.signature.XmlSignatureException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns every unhandled failure into a controlled response.
 *
 * <h2>What belongs here and what does not</h2>
 * This handler deals with requests that could not become payments: a payload that will not
 * parse, a missing header, an unsupported content type, an internal fault. It does not
 * deal with business rejections. A message that was understood and then refused receives a
 * signed pacs.002 with an ISO reason code, decided in the service layer, and never reaches
 * this class.
 *
 * <p>The distinction is the reason a rejection is not modelled as an exception. Using
 * exceptions for expected outcomes would put the most important behaviour in the service —
 * telling a bank why its payment was refused — in an error handler, where it would be
 * indistinguishable from a genuine fault.
 *
 * <h2>Why no error returns a pacs.002</h2>
 * A status report must quote the identifiers of the message it reports on. In every case
 * handled here those identifiers were never successfully read, so a pacs.002 would have to
 * invent them.
 *
 * <h2>What is never disclosed</h2>
 * No stack trace, no exception type, no internal path reaches the caller. Those describe
 * how the service is built rather than what the caller did wrong, and they are exactly
 * what an attacker probes for. The full detail is logged against the correlation id, which
 * the caller does receive, so nothing is lost to whoever is entitled to see it.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ErrorResponseWriter errorResponseWriter;
    private final Clock clock;

    public GlobalExceptionHandler(ErrorResponseWriter errorResponseWriter, Clock clock) {
        this.errorResponseWriter = errorResponseWriter;
        this.clock = clock;
    }

    /**
     * A well-formed document that does not conform to the pacs.008 schema.
     *
     * <p>Every violation found is returned, so a sender correcting the message learns
     * everything that is wrong in one exchange. Schema violations name elements and
     * constraints, never element content, so they are safe to disclose.
     */
    @ExceptionHandler(SchemaValidationException.class)
    public ResponseEntity<String> handleSchemaValidation(SchemaValidationException e) {
        log.warn("Rejected a message that does not conform to the schema: {} violation(s)",
                e.getViolations().size());
        return respond(HttpStatus.BAD_REQUEST, "SCHEMA_VALIDATION_FAILED",
                "The message does not conform to the pacs.008 schema", e.getViolations());
    }

    /** A payload that is empty, oversized, not well formed, or carries a DOCTYPE. */
    @ExceptionHandler(XmlProcessingException.class)
    public ResponseEntity<String> handleXmlProcessing(XmlProcessingException e) {
        // The parser's own message describes the structural fault and names no content.
        log.warn("Rejected an unreadable payload: {}", e.getMessage());
        return respond(HttpStatus.BAD_REQUEST, "MALFORMED_XML", e.getMessage(), List.of());
    }

    /** An idempotency key presented with a payload different from the original. */
    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<String> handleIdempotencyConflict(IdempotencyConflictException e) {
        log.warn("Rejected a request reusing an idempotency key with a different payload");
        return respond(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT", e.getMessage(),
                List.of("Use a new idempotency key for a different payment"));
    }

    /** A required header was not supplied. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<String> handleMissingHeader(MissingRequestHeaderException e) {
        log.warn("Rejected a request missing the {} header", e.getHeaderName());
        return respond(HttpStatus.BAD_REQUEST, "MISSING_REQUIRED_HEADER",
                "Required header '%s' was not supplied".formatted(e.getHeaderName()),
                List.of());
    }

    /** A content type this endpoint does not accept. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException e) {
        log.warn("Rejected a request with unsupported content type {}", e.getContentType());
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "This endpoint accepts application/xml", List.of());
    }

    /** A body the framework could not read at all, most often an empty one. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleUnreadableBody(HttpMessageNotReadableException e) {
        log.warn("Rejected a request whose body could not be read");
        return respond(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "The request body could not be read", List.of());
    }

    /**
     * A database constraint refused the write.
     *
     * <p>In practice this is the concurrent-creation race: two requests naming the same
     * previously unknown customer, or carrying the same transaction, both reaching the
     * database at once. The constraint is doing its job. A retry under the same idempotency
     * key is safe and will succeed, which is what the caller is told.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("A database constraint refused a concurrent write");
        return respond(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The request conflicted with another being processed at the same time",
                List.of("Retry with the same idempotency key"));
    }

    /** The response could not be signed. */
    @ExceptionHandler(XmlSignatureException.class)
    public ResponseEntity<String> handleSignatureFailure(XmlSignatureException e) {
        // An unsigned status report is not a lesser response that could be sent anyway: a
        // counterparty cannot act on one it is unable to authenticate.
        log.error("Unable to sign a status report", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "SIGNING_FAILED",
                "The status report could not be signed", List.of());
    }

    /**
     * Anything not anticipated.
     *
     * <p>Logged in full, including the stack trace, because an operator needs it. Reported
     * to the caller as nothing but a correlation id, because they do not.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleUnexpected(Exception e) {
        log.error("Unexpected failure while processing a request", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "The request could not be processed", List.of());
    }

    private ResponseEntity<String> respond(HttpStatus status, String code, String message,
                                           List<String> details) {
        ErrorResponse body = new ErrorResponse(code, message, CorrelationId.current(),
                DateTimeFormatter.ISO_INSTANT.format(Instant.now(clock)), details);

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_XML)
                .header(CorrelationIdHeaders.CORRELATION_ID, CorrelationId.current())
                .body(errorResponseWriter.write(body));
    }

    /** Header names shared with the controller. */
    static final class CorrelationIdHeaders {
        static final String CORRELATION_ID = "X-Correlation-Id";

        private CorrelationIdHeaders() {
        }
    }
}
