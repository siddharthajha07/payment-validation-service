package com.rbc.paymentvalidation.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearContext() {
        MDC.clear();
    }

    @Test
    @DisplayName("uses the identifier the caller supplied")
    void usesSuppliedIdentifier() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "caller-supplied-id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.HEADER))
                .isEqualTo("caller-supplied-id");
    }

    @Test
    @DisplayName("generates an identifier when the caller supplies none")
    void generatesIdentifierWhenAbsent() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isNotBlank();
    }

    @Test
    @DisplayName("publishes the identifier to the logging context for the whole request")
    void publishesIdentifierToLoggingContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "visible-to-logging");

        // Captures what the diagnostic context held while the request was being handled.
        String[] observed = new String[1];
        FilterChain capturingChain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res)
                    throws IOException, ServletException {
                observed[0] = MDC.get(CorrelationId.MDC_KEY);
            }
        };

        filter.doFilter(request, new MockHttpServletResponse(), capturingChain);

        assertThat(observed[0]).isEqualTo("visible-to-logging");
    }

    @Test
    @DisplayName("clears the identifier so a pooled thread cannot carry it to the next request")
    void clearsIdentifierAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "first-request");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("clears the identifier even when the request fails")
    void clearsIdentifierAfterFailure() {
        // Without the finally block, one failed request would leave its identifier attached
        // to every subsequent request that reused the thread. Those logs would be actively
        // misleading, which is worse than having no identifier at all.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "failing-request");
        FilterChain failingChain = (req, res) -> {
            throw new ServletException("processing failed");
        };

        try {
            filter.doFilter(request, new MockHttpServletResponse(), failingChain);
        } catch (Exception expected) {
            // The failure is the point of the test.
        }

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("truncates an over-long identifier to what the database can store")
    void truncatesOverLongIdentifier() {
        String tooLong = "x".repeat(200);

        assertThat(CorrelationId.resolve(tooLong)).hasSize(CorrelationId.MAX_LENGTH);
    }
}
