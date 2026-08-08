package com.rbc.paymentvalidation.repository;

import com.rbc.paymentvalidation.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Storage for already-answered requests, keyed by the client's idempotency key.
 *
 * The key is the identifier, so a replay is a primary-key lookup rather than a scan, which is
 * the cheapest possible check on the hot path.
 */
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
}
