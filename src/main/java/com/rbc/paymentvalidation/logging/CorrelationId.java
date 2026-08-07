package com.rbc.paymentvalidation.logging;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * The identifier that ties together everything one request produces.
 *
 * <h2>Why the mapped diagnostic context</h2>
 * The MDC is a per-thread map the logging framework consults when formatting each line.
 * Placing the correlation id there once means every subsequent log statement carries it
 * without any method having to accept or forward it. The alternative — threading an
 * identifier through every signature in the codebase — couples classes to a logging
 * concern they otherwise have no interest in.
 *
 * <p>The same identifier is written to the {@code payment} and {@code audit_event} tables
 * and returned in a response header, so one value connects the caller's report of a
 * problem, the log lines, and the database rows.
 */
public final class CorrelationId {

    /** The key under which the identifier is published to the logging framework. */
    public static final String MDC_KEY = "correlationId";

    /** The width of the column that stores it, and therefore the maximum accepted. */
    public static final int MAX_LENGTH = 36;

    private CorrelationId() {
    }

    /**
     * @return the identifier for the request being handled on this thread, or a newly
     *         generated one if none has been set. Never {@code null}: code that records an
     *         audit event should not have to guard against a missing identifier, and an
     *         event filed under a fresh identifier is more useful than none at all.
     */
    public static String current() {
        String existing = MDC.get(MDC_KEY);
        return existing == null || existing.isBlank() ? generate() : existing;
    }

    public static void set(String correlationId) {
        MDC.put(MDC_KEY, correlationId);
    }

    /**
     * Removes the identifier from this thread.
     *
     * <p>Essential rather than tidy. Servlet containers pool and reuse threads, so an
     * identifier left behind would attach itself to the next, unrelated request — and the
     * resulting logs would be actively misleading, which is worse than having none.
     */
    public static void clear() {
        MDC.remove(MDC_KEY);
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }

    /**
     * @return the caller's identifier if usable, otherwise a generated one. A caller that
     *         supplies its own can line our logs up with theirs; one that does not still
     *         gets a request that can be traced end to end.
     */
    public static String resolve(String supplied) {
        if (supplied == null || supplied.isBlank()) {
            return generate();
        }
        String trimmed = supplied.trim();
        return trimmed.length() > MAX_LENGTH ? trimmed.substring(0, MAX_LENGTH) : trimmed;
    }
}
