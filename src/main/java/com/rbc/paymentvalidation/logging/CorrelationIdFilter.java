package com.rbc.paymentvalidation.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Establishes a correlation identifier for every request, before anything else runs.
 *
 * <h2>Why a filter rather than the controller</h2>
 * A filter wraps the entire request, including the parts of it that never reach a
 * controller: a malformed body rejected by the framework, an unsupported content type, an
 * exception thrown before dispatch. Those are exactly the requests somebody will later ask
 * about, so they are the ones that most need to be traceable. Setting the identifier in the
 * controller would leave them anonymous.
 *
 * <p>{@code HIGHEST_PRECEDENCE} places it first in the chain, so no other filter logs
 * anything before the identifier exists.
 *
 * <h2>Why the identifier is echoed back</h2>
 * A caller reporting a problem can quote the header from the response they received, and
 * that single value locates every log line, payment row and audit event belonging to the
 * request. Without it, investigating a complaint means searching by timestamp and hoping.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = CorrelationId.resolve(request.getHeader(HEADER));
        CorrelationId.set(correlationId);
        // Set before the chain runs: an error response produced downstream must carry the
        // header too, and by then the response may already be committed.
        response.setHeader(HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Servlet threads are pooled and reused. An identifier left behind would be
            // attached to the next, unrelated request, producing logs that actively
            // mislead — worse than logs with no identifier at all.
            CorrelationId.clear();
        }
    }
}
