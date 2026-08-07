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
 * Recognises requests already answered, and answers them the same way.
 *
 * A client whose connection dropped cannot know whether its payment was processed, and an
 * idempotency key removes the dilemma. The stored response is replayed rather than
 * regenerated, because a fresh one would carry a new timestamp and therefore a new signature.
 *
 * The request is hashed to tell an honest retry from a key reused with different content. That
 * also keeps names and account numbers out of this table.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

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
