package com.rbc.paymentvalidation.repository;

import com.rbc.paymentvalidation.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Storage for already-answered requests, keyed by the client-supplied idempotency key.
 *
 * <p>The key is the entity's identifier, so a replay is a primary-key lookup rather than a
 * scan — the cheapest possible check on the hot path, performed before any parsing work.
 */
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
}
