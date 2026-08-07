package com.rbc.paymentvalidation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rbc.paymentvalidation.domain.AuditEventType;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Proves that audit events survive the rollback of the work they describe.
 *
 * <p>{@code AuditService} declares {@code REQUIRES_NEW}, but an annotation nobody exercises
 * is a claim, not a guarantee — and this one only takes effect through Spring's proxy, so
 * it cannot be verified by constructing the service directly. This test therefore uses the
 * real application context and a genuine rollback.
 *
 * <p>What is at stake: if audit events shared the caller's transaction, then whenever
 * processing failed the trail would lose all record that the attempt had ever been made.
 * A failed attempt is precisely what an auditor most wants to see.
 */
@SpringBootTest
class AuditServiceTransactionTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("keeps audit events written inside a transaction that then rolls back")
    void keepsEventsWrittenInsideARolledBackTransaction() {
        // A plain UUID: the correlation id column is sized for one, and a longer value
        // would fail on insert rather than testing what this method is about.
        String correlationId = UUID.randomUUID().toString();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            auditService.record(correlationId, AuditEventType.MESSAGE_RECEIVED,
                    "Request accepted for processing");
            throw new IllegalStateException("Processing failed after the event was recorded");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(auditService.historyOf(correlationId))
                .as("the audit trail must record the attempt even though the work rolled back")
                .hasSize(1);
    }

    @Test
    @DisplayName("returns the history of a request in the order it happened")
    void returnsHistoryInOrder() {
        String correlationId = UUID.randomUUID().toString();

        auditService.record(correlationId, AuditEventType.MESSAGE_RECEIVED, "Received");
        auditService.record(correlationId, AuditEventType.MESSAGE_VALIDATED, "Schema valid");
        auditService.record(correlationId, AuditEventType.VALIDATION_PASSED, "Rules passed");

        assertThat(auditService.historyOf(correlationId))
                .extracting(event -> event.getEventType().name())
                .containsExactly("MESSAGE_RECEIVED", "MESSAGE_VALIDATED", "VALIDATION_PASSED");
    }
}
