package com.rbc.paymentvalidation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rbc.paymentvalidation.domain.IdempotencyRecord;
import com.rbc.paymentvalidation.repository.IdempotencyRecordRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class IdempotencyServiceTest {

    @Autowired
    private IdempotencyRecordRepository repository;

    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new IdempotencyService(repository);
    }

    private byte[] payload(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("hashes identical payloads to the same value")
    void hashesIdenticalPayloadsIdentically() {
        assertThat(idempotencyService.hash(payload("<Message/>")))
                .isEqualTo(idempotencyService.hash(payload("<Message/>")));
    }

    @Test
    @DisplayName("hashes different payloads to different values")
    void hashesDifferentPayloadsDifferently() {
        assertThat(idempotencyService.hash(payload("<Message/>")))
                .isNotEqualTo(idempotencyService.hash(payload("<Message />")));
    }

    @Test
    @DisplayName("produces a SHA-256 hash, which is 64 hex characters")
    void producesSha256() {
        assertThat(idempotencyService.hash(payload("<Message/>")))
                .hasSize(64)
                .matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("finds nothing for a key never seen before")
    void findsNothingForANewKey() {
        assertThat(idempotencyService.findPreviousResponse("new-key", "any-hash")).isEmpty();
    }

    @Test
    @DisplayName("replays the stored response for a genuine retry")
    void replaysStoredResponseForGenuineRetry() {
        // A client whose connection dropped retries with the same key and must get the
        // original answer, not a second payment.
        String hash = idempotencyService.hash(payload("<Message/>"));
        idempotencyService.recordResponse("key-1", hash, "<Response/>", 200, "TX-1", "corr-1");

        assertThat(idempotencyService.findPreviousResponse("key-1", hash))
                .get()
                .extracting(IdempotencyRecord::getResponseXml)
                .isEqualTo("<Response/>");
    }

    @Test
    @DisplayName("replays the original status as well as the original body")
    void replaysOriginalStatus() {
        // A retry should be indistinguishable from the exchange it repeats, including a
        // rejection: replaying a 422 as a 200 would tell the caller their payment succeeded.
        String hash = idempotencyService.hash(payload("<Message/>"));
        idempotencyService.recordResponse("key-1", hash, "<Rejected/>", 422, "TX-1", "corr-1");

        assertThat(idempotencyService.findPreviousResponse("key-1", hash))
                .get()
                .extracting(IdempotencyRecord::getResponseStatus)
                .isEqualTo(422);
    }

    @Test
    @DisplayName("refuses a key reused with a different payload")
    void refusesKeyReusedWithDifferentPayload() {
        // The dangerous case. Replaying would hand the caller a status report about
        // somebody else's payment; processing afresh would defeat the key entirely.
        idempotencyService.recordResponse("key-1", idempotencyService.hash(payload("<A/>")),
                "<Response/>", 200, "TX-1", "corr-1");

        assertThatThrownBy(() -> idempotencyService.findPreviousResponse("key-1",
                idempotencyService.hash(payload("<B/>"))))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    @DisplayName("stores the hash rather than the payload itself")
    void storesTheHashNotThePayload() {
        // The payload carries customer names and account numbers. A hash answers the only
        // question this table needs to ask without retaining any of them.
        idempotencyService.recordResponse("key-1",
                idempotencyService.hash(payload("<Dbtr><Nm>ACME</Nm></Dbtr>")),
                "<Response/>", 200, "TX-1", "corr-1");

        assertThat(repository.findById("key-1"))
                .get()
                .extracting(IdempotencyRecord::getRequestHash)
                .satisfies(hash -> assertThat((String) hash).doesNotContain("ACME"));
    }
}
