package com.rbc.paymentvalidation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.rbc.paymentvalidation.testsupport.SampleMessages;
import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end tests for requests that never become payments.
 *
 * <p>Each of these returns an {@code ErrorResponse} rather than a pacs.002, and the reason
 * is the same in every case: a status report has to quote the identifiers of the message it
 * reports on, and in none of these situations were those identifiers successfully read.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ErrorHandlingIT.FixedClockConfiguration.class)
class ErrorHandlingIT {

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedClock() {
            return ValidationFixtures.FIXED_CLOCK;
        }
    }

    /**
     * Injected rather than built from the context.
     *
     * <p>{@code MockMvcBuilders.webAppContextSetup(...).build()} wires the dispatcher but
     * <em>not</em> the servlet filters, so {@code CorrelationIdFilter} would never run and
     * these tests would report a missing correlation header as an application fault when it
     * is really a gap in the test setup.
     */
    @Autowired
    private MockMvc mockMvc;

    private MvcResult submit(String idempotencyKey, byte[] body) throws Exception {
        return mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_XML)
                        .header(PaymentController.IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                        .header(PaymentController.SENDER_INSTITUTION_HEADER, "BANKA000")
                        .content(body))
                .andReturn();
    }

    private String bodyOf(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("returns 400 for XML that is not well formed")
    void returnsBadRequestForMalformedXml() throws Exception {
        MvcResult result = submit("err-malformed",
                "<Message><unclosed></Message>".getBytes(StandardCharsets.UTF_8));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(bodyOf(result)).contains("<Code>MALFORMED_XML</Code>");
    }

    @Test
    @DisplayName("returns 400 for an external entity attack, disclosing nothing")
    void returnsBadRequestForXxeAttack() throws Exception {
        // The end-to-end proof of the parser hardening: the file is never read, and the
        // response carries no trace of what was attempted.
        String attack = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE message [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
                <message>&xxe;</message>
                """;

        MvcResult result = submit("err-xxe", attack.getBytes(StandardCharsets.UTF_8));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(bodyOf(result)).doesNotContain("root:");
    }

    @Test
    @DisplayName("returns 400 listing every schema violation at once")
    void returnsBadRequestListingEveryViolation() throws Exception {
        // A sender repairing a message should learn everything that is wrong in one
        // exchange rather than one fault per round trip.
        byte[] body = SampleMessages.pacs008ValidAsText()
                .replace("<BizMsgIdr>MIB621200494113</BizMsgIdr>", "")
                .replace("<MsgId>MIB621200494113</MsgId>", "")
                .getBytes(StandardCharsets.UTF_8);

        MvcResult result = submit("err-schema", body);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(bodyOf(result))
                .contains("<Code>SCHEMA_VALIDATION_FAILED</Code>")
                .contains("<Detail>");
    }

    @Test
    @DisplayName("returns 400 when a required header is absent")
    void returnsBadRequestWhenHeaderMissing() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_XML)
                        .header(PaymentController.SENDER_INSTITUTION_HEADER, "BANKA000")
                        .content(SampleMessages.pacs008Valid()))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(bodyOf(result))
                .contains("<Code>MISSING_REQUIRED_HEADER</Code>")
                .contains("X-Idempotency-Key");
    }

    @Test
    @DisplayName("returns 415 for a content type this endpoint does not accept")
    void returnsUnsupportedMediaType() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(PaymentController.IDEMPOTENCY_KEY_HEADER, "err-media-type")
                        .header(PaymentController.SENDER_INSTITUTION_HEADER, "BANKA000")
                        .content("{}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(415);
    }

    @Test
    @DisplayName("returns 400 for an empty body")
    void returnsBadRequestForEmptyBody() throws Exception {
        MvcResult result = submit("err-empty", new byte[0]);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("returns 409 when an idempotency key is reused with a different payload")
    void returnsConflictForReusedIdempotencyKey() throws Exception {
        // Replaying would hand the caller a status report about a different payment;
        // processing afresh would defeat the key. Refusing is the only safe answer.
        byte[] first = SampleMessages.pacs008ValidAsText()
                .replace("<TxId>B621200494113</TxId>", "<TxId>TX-CONFLICT-A</TxId>")
                .getBytes(StandardCharsets.UTF_8);
        byte[] second = SampleMessages.pacs008ValidAsText()
                .replace("<TxId>B621200494113</TxId>", "<TxId>TX-CONFLICT-B</TxId>")
                .getBytes(StandardCharsets.UTF_8);

        submit("err-reused-key", first);
        MvcResult result = submit("err-reused-key", second);

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(bodyOf(result)).contains("<Code>IDEMPOTENCY_KEY_CONFLICT</Code>");
    }

    @Test
    @DisplayName("never discloses stack traces or internal class names")
    void neverDisclosesInternals() throws Exception {
        // These describe how the service is built rather than what the caller did wrong,
        // and are exactly what an attacker probes for.
        MvcResult result = submit("err-no-internals",
                "<Message><unclosed></Message>".getBytes(StandardCharsets.UTF_8));

        assertThat(bodyOf(result))
                .doesNotContain("Exception")
                .doesNotContain("com.rbc.paymentvalidation")
                .doesNotContain("at java.");
    }

    @Test
    @DisplayName("carries the correlation id on an error, so the caller can quote it")
    void carriesCorrelationIdOnError() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_XML)
                        .header(PaymentController.IDEMPOTENCY_KEY_HEADER, "err-correlation")
                        .header(PaymentController.SENDER_INSTITUTION_HEADER, "BANKA000")
                        .header(PaymentController.CORRELATION_ID_HEADER, "trace-me")
                        .content("<Message><unclosed></Message>".getBytes(StandardCharsets.UTF_8)))
                .andReturn();

        assertThat(result.getResponse().getHeader(PaymentController.CORRELATION_ID_HEADER))
                .isEqualTo("trace-me");
        assertThat(bodyOf(result)).contains("<CorrelationId>trace-me</CorrelationId>");
    }

    @Test
    @DisplayName("assigns a correlation id even to a request that never reaches the controller")
    void assignsCorrelationIdToRejectedRequest() throws Exception {
        // The filter runs before dispatch precisely so that these requests — the ones
        // somebody is most likely to ask about later — are traceable too.
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_XML)
                        .header(PaymentController.SENDER_INSTITUTION_HEADER, "BANKA000")
                        .content(SampleMessages.pacs008Valid()))
                .andReturn();

        assertThat(result.getResponse().getHeader(PaymentController.CORRELATION_ID_HEADER))
                .isNotBlank();
    }
}
