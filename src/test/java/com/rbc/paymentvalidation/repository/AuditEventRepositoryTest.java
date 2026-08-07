package com.rbc.paymentvalidation.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.domain.AuditEvent;
import com.rbc.paymentvalidation.domain.AuditEventType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * Tests that the audit trail is genuinely append-only.
 *
 * <p>Two of these tests exist to prove claims that would otherwise be only comments: that
 * no operation exists to erase an event, and that an attempt to modify one has no effect
 * on what is stored.
 */
@DataJpaTest
class AuditEventRepositoryTest {

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("exposes no operation capable of deleting or modifying an event")
    void exposesNoDestructiveOperation() {
        // Extending Repository rather than JpaRepository means the interface has exactly
        // the methods declared on it. Had it extended JpaRepository, delete, deleteAll and
        // deleteAllInBatch would all be reachable, and this assertion would fail.
        assertThat(AuditEventRepository.class.getMethods())
                .extracting(Method::getName)
                .noneMatch(name -> name.startsWith("delete") || name.startsWith("remove"));
    }

    @Test
    @DisplayName("does not persist a modification to a stored event")
    void doesNotPersistModification() throws Exception {
        AuditEvent saved = auditEventRepository.save(new AuditEvent(
                "corr-1", AuditEventType.VALIDATION_FAILED, 42L, "Sender equals receiver"));
        entityManager.flush();
        entityManager.clear();

        // The entity has no setters, so tampering requires reflection. Even then, every
        // column is mapped updatable = false, so Hibernate emits no UPDATE for them.
        AuditEvent managed = entityManager.find(AuditEvent.class, saved.getId());
        Field detail = AuditEvent.class.getDeclaredField("detail");
        detail.setAccessible(true);
        detail.set(managed, "tampered");
        entityManager.flush();
        entityManager.clear();

        AuditEvent reloaded = entityManager.find(AuditEvent.class, saved.getId());
        assertThat(reloaded.getDetail()).isEqualTo("Sender equals receiver");
    }

    @Test
    @DisplayName("returns the history of one request in the order it happened")
    void returnsHistoryInOrder() {
        auditEventRepository.save(new AuditEvent(
                "corr-1", AuditEventType.MESSAGE_RECEIVED, null, "Request accepted"));
        auditEventRepository.save(new AuditEvent(
                "corr-1", AuditEventType.MESSAGE_VALIDATED, null, "Schema valid"));
        auditEventRepository.save(new AuditEvent(
                "corr-1", AuditEventType.VALIDATION_PASSED, null, "All rules passed"));

        assertThat(auditEventRepository.findByCorrelationIdOrderByOccurredAtAsc("corr-1"))
                .extracting(AuditEvent::getEventType)
                .containsExactly(
                        AuditEventType.MESSAGE_RECEIVED,
                        AuditEventType.MESSAGE_VALIDATED,
                        AuditEventType.VALIDATION_PASSED);
    }

    @Test
    @DisplayName("records events before any payment row exists")
    void recordsEventsWithoutAPayment() {
        // The trail starts when the request arrives, which is before a payment has been
        // created — and must still work when processing fails before one ever is. A
        // foreign key here would make that impossible.
        AuditEvent saved = auditEventRepository.save(new AuditEvent(
                "corr-1", AuditEventType.MESSAGE_RECEIVED, null, "Request accepted"));

        assertThat(saved.getPaymentId()).isNull();
        assertThat(saved.getOccurredAt()).isNotNull();
    }

    @Test
    @DisplayName("finds everything recorded about one payment")
    void findsEventsForOnePayment() {
        auditEventRepository.save(new AuditEvent(
                "corr-1", AuditEventType.PAYMENT_RECORDED, 42L, "Payment stored"));
        auditEventRepository.save(new AuditEvent(
                "corr-2", AuditEventType.PAYMENT_RECORDED, 43L, "Payment stored"));

        assertThat(auditEventRepository.findByPaymentIdOrderByOccurredAtAsc(42L)).hasSize(1);
    }
}
