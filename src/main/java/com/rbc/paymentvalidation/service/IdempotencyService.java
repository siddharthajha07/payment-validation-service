package com.rbc.paymentvalidation.service;

import com.rbc.paymentvalidation.domain.IdempotencyRecord;
import com.rbc.paymentvalidation.repository.IdempotencyRecordRepository;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recognises requests that have already been answered, and answers them the same way.
 *
 * <h2>The problem this solves</h2>
 * A client sends a payment, the connection drops before the response arrives, and the
 * client has no way to know whether the payment was processed. Its only safe options are
 * to retry and risk paying twice, or not to retry and risk not paying at all. An
 * idempotency key removes the dilemma: the client retries with the same key and gets the
 * original answer back.
 *
 * <h2>Why the stored response is replayed rather than regenerated</h2>
 * The response carries a digital signature over its exact bytes, and a regenerated report
 * would have a new identifier and timestamp and therefore a different signature. A client
 * comparing the two would see two different documents for one payment. Storing the
 * response makes a replay genuinely identical.
 *
 * <h2>Why the request is hashed</h2>
 * The hash distinguishes an honest retry from a key reused by mistake with different
 * content. The first is replayed; the second is refused, because replaying it would return
 * a status report about a different payment. Hashing rather than storing the body also
 * keeps customer names and account numbers out of this table.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    /**
     * @param payload the raw request body
     * @return the SHA-256 of the body, hex encoded
     */
    public String hash(byte[] payload) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required of every Java platform; its absence is not recoverable.
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /**
     * Looks for a previous answer to this request.
     *
     * @param idempotencyKey the client-supplied key
     * @param requestHash    the hash of the body now being presented
     * @return the original response if this is a genuine retry, otherwise empty
     * @throws IdempotencyConflictException if the key was used with a different body
     */
    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findPreviousResponse(String idempotencyKey,
                                                            String requestHash) {
        Optional<IdempotencyRecord> existing =
                idempotencyRecordRepository.findById(idempotencyKey);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyRecord record = existing.get();
        if (!record.matchesRequest(requestHash)) {
            log.warn("Idempotency key reused with a different payload");
            throw new IdempotencyConflictException(idempotencyKey);
        }

        log.info("Replaying the stored response for a repeated request");
        return existing;
    }

    /** Stores the answer so that a later retry receives exactly this response. */
    @Transactional
    public void recordResponse(String idempotencyKey, String requestHash, String responseXml,
                               int responseStatus, String transactionId, String correlationId) {
        idempotencyRecordRepository.save(new IdempotencyRecord(idempotencyKey, requestHash,
                responseXml, responseStatus, transactionId, correlationId));
    }
}
