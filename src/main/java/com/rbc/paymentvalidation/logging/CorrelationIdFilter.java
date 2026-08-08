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
 * Establishes a correlation id for every request, before anything else runs.
 *
 * A filter rather than the controller, because it wraps the requests that never reach a
 * controller: a malformed body, a wrong content type, an exception before dispatch. Those are
 * the ones somebody asks about later, so they are the ones that most need to be traceable.
 *
 * The id is echoed back so a caller reporting a problem can quote it. Clearing it in the
 * finally block is not tidiness: servlet threads are pooled, and an id left behind would
 * attach to the next unrelated request, which is worse than having none.
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
