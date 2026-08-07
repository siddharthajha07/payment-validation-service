package com.rbc.paymentvalidation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.rbc.paymentvalidation.repository.AuditEventRepository;
import com.rbc.paymentvalidation.repository.CustomerRepository;
import com.rbc.paymentvalidation.repository.PaymentRepository;
import com.rbc.paymentvalidation.testsupport.SampleMessages;
import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import com.rbc.paymentvalidation.xml.SecureXmlParser;
import com.rbc.paymentvalidation.xml.signature.XmlSignatureService;
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
 * End-to-end tests through the HTTP endpoint against the real application.
 *
 * <p>Named {@code *IT} so it runs under Failsafe, after packaging, rather than in the fast
 * unit-test cycle.
 *
 * <p>The clock is replaced with a fixed one. The supplied sample settles on 2026-07-31, so
 * a test against the real clock would pass only until that date passed and then start
 * failing for a reason unrelated to any change in the code.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PaymentApiIT.FixedClockConfiguration.class)
class PaymentApiIT {

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
     * every response would lack its correlation header — a failure of the test setup that
     * looks exactly like a failure of the application.
     */
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private XmlSignatureService signatureService;

    @Autowired
    private SecureXmlParser parser;

    /**
     * The conformant message carrying a transaction identifier unique to one test.
     *
     * <p>{@code @SpringBootTest} shares one application context and one database across the
     * whole class, and nothing is rolled back between tests. Since the service is designed
     * to reject a transaction identifier it has already processed — including one stored by
     * a rejection — every test must submit its own, exactly as every real payment carries
     * its own. Sharing one would make the tests pass or fail according to the order they
     * happened to run in.
     */
    private byte[] messageWithTransactionId(String transactionId) {
        return SampleMessages.pacs008ValidAsText()
                .replace("<TxId>B621200494113</TxId>", "<TxId>" + transactionId + "</TxId>")
                .getBytes(StandardCharsets.UTF_8);
    }

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
    @DisplayName("accepts a conformant message and returns a signed ACCP report")
    void acceptsConformantMessage() throws Exception {
        MvcResult result = submit("it-accept", messageWithTransactionId("TX-ACCEPT"));

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(bodyOf(result)).contains("<pacs:GrpSts>ACCP</pacs:GrpSts>");
        assertThat(paymentRepository.findByTransactionId("TX-ACCEPT")).isPresent();
    }

    @Test
    @DisplayName("returns a response whose signature verifies")
    void returnsAVerifiableSignature() throws Exception {
        // The response is meaningless to a counterparty unless the signature holds over the
        // exact bytes returned, so it is parsed back from the wire and verified as received.
        MvcResult result = submit("it-signature", messageWithTransactionId("TX-SIGNATURE"));

        assertThat(signatureService.verify(parser.parse(
                bodyOf(result).getBytes(StandardCharsets.UTF_8)))).isTrue();
    }

    @Test
    @DisplayName("creates the customer and audit trail for an accepted payment")
    void createsCustomerAndAuditTrail() throws Exception {
        submit("it-customer", messageWithTransactionId("TX-CUSTOMER"));

        assertThat(customerRepository.findByCustomerReference("6075857")).isPresent();
        assertThat(auditEventRepository.findByPaymentIdOrderByOccurredAtAsc(
                paymentRepository.findByTransactionId("TX-CUSTOMER").orElseThrow().getId()))
                .isNotEmpty();
    }

    @Test
    @DisplayName("replays the identical response for a repeated request")
    void replaysRepeatedRequest() throws Exception {
        byte[] message = messageWithTransactionId("TX-REPLAY");
        MvcResult first = submit("it-replay", message);
        MvcResult second = submit("it-replay", message);

        assertThat(second.getResponse().getStatus()).isEqualTo(first.getResponse().getStatus());
        assertThat(second.getResponse().getHeader(PaymentController.REPLAY_HEADER))
                .isEqualTo("true");
        // Byte-identical, because the response is stored rather than regenerated: a fresh
        // report would carry a new identifier and timestamp and therefore a new signature.
        assertThat(bodyOf(second)).isEqualTo(bodyOf(first));
    }

    @Test
    @DisplayName("rejects a message whose sender and receiver are the same institution")
    void rejectsIdenticalSenderAndReceiver() throws Exception {
        byte[] body = new String(messageWithTransactionId("TX-SAME-PARTIES"),
                StandardCharsets.UTF_8)
                .replace("<BICFI>CBANK0IPS</BICFI>", "<BICFI>BANKA000</BICFI>")
                .getBytes(StandardCharsets.UTF_8);

        MvcResult result = submit("it-same-parties", body);

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(bodyOf(result))
                .contains("<pacs:GrpSts>RJCT</pacs:GrpSts>")
                .contains("<pacs:Cd>AGNT</pacs:Cd>");
    }

    @Test
    @DisplayName("rejects an account number inconsistent with its institution")
    void rejectsWrongAccountPrefix() throws Exception {
        byte[] body = new String(messageWithTransactionId("TX-BAD-PREFIX"),
                StandardCharsets.UTF_8)
                .replace("<Id>FI2003135</Id>", "<Id>XX2003135</Id>")
                .getBytes(StandardCharsets.UTF_8);

        MvcResult result = submit("it-bad-prefix", body);

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(bodyOf(result)).contains("<pacs:Cd>AC01</pacs:Cd>");
    }

    @Test
    @DisplayName("signs rejections as well as acceptances")
    void signsRejectionsToo() throws Exception {
        // A rejection is a business outcome the counterparty must be able to trust and act
        // on, so it carries the same signature an acceptance does.
        byte[] body = new String(messageWithTransactionId("TX-SIGNED-REJECTION"),
                StandardCharsets.UTF_8)
                .replace("Ccy=\"BBD\"", "Ccy=\"USD\"")
                .getBytes(StandardCharsets.UTF_8);

        MvcResult result = submit("it-signed-rejection", body);

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(signatureService.verify(parser.parse(
                bodyOf(result).getBytes(StandardCharsets.UTF_8)))).isTrue();
    }

    @Test
    @DisplayName("rejects the supplied sample, which breaks two documented rules")
    void rejectsTheSuppliedSampleAsProvided() throws Exception {
        // The sample carries unprefixed account numbers and five-digit transit identifiers
        // where the assessment specifies three. The rules are implemented as written and
        // the conflict is documented rather than accommodated.
        MvcResult result = submit("it-original-sample", SampleMessages.pacs008Sample());

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(bodyOf(result)).contains("<pacs:GrpSts>RJCT</pacs:GrpSts>");
    }

    @Test
    @DisplayName("echoes a caller-supplied correlation id")
    void echoesSuppliedCorrelationId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_XML)
                        .header(PaymentController.IDEMPOTENCY_KEY_HEADER, "it-correlation")
                        .header(PaymentController.SENDER_INSTITUTION_HEADER, "BANKA000")
                        .header(PaymentController.CORRELATION_ID_HEADER, "caller-supplied-id")
                        .content(messageWithTransactionId("TX-CORRELATION")))
                .andReturn();

        assertThat(result.getResponse().getHeader(PaymentController.CORRELATION_ID_HEADER))
                .isEqualTo("caller-supplied-id");
    }

    @Test
    @DisplayName("generates a correlation id when the caller supplies none")
    void generatesCorrelationIdWhenAbsent() throws Exception {
        MvcResult result = submit("it-generated-correlation",
                messageWithTransactionId("TX-GENERATED-CORRELATION"));

        assertThat(result.getResponse().getHeader(PaymentController.CORRELATION_ID_HEADER))
                .isNotBlank();
    }
}
