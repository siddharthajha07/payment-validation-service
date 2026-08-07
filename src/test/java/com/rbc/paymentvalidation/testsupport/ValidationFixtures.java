package com.rbc.paymentvalidation.testsupport;

import com.rbc.paymentvalidation.domain.Institution;
import com.rbc.paymentvalidation.validation.ValidationContext;
import com.rbc.paymentvalidation.validation.ValidationProperties;
import com.rbc.paymentvalidation.xml.Pacs008Unmarshaller;
import com.rbc.paymentvalidation.xml.SecureXmlParser;
import com.rbc.paymentvalidation.xml.model.envelope.Pacs008Message;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Shared building blocks for the validation tests.
 *
 * <p>Tests start from a real, parsed message and mutate the one value under test. Building
 * message objects field by field would be quicker to write but would prove less: a test
 * that constructs exactly the shape it expects cannot discover that the real message has a
 * different shape.
 */
public final class ValidationFixtures {

    /**
     * A fixed point in time, one day before the settlement date carried by the sample.
     *
     * <p>Every date-sensitive test uses this rather than the real clock. The sample settles
     * on 2026-07-31; anchoring "today" at 2026-07-30 makes that date exactly one day ahead,
     * so it sits inside the two-day window — and will still sit inside it whenever these
     * tests are run, which would not be true of a test written against the real clock.
     */
    public static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-30T12:00:00Z"), ZoneOffset.UTC);

    /** The production configuration values, so tests exercise the shipped policy. */
    public static final ValidationProperties PROPERTIES =
            new ValidationProperties("BBD", 2, 3, 2);

    public static final String SENDER_BIC = "BANKA000";
    public static final String RECEIVER_BIC = "CBANK0IPS";
    public static final String CORRELATION_ID = "test-correlation-id";

    private static final SecureXmlParser PARSER = new SecureXmlParser(1_048_576);
    private static final Pacs008Unmarshaller UNMARSHALLER = new Pacs008Unmarshaller();

    private ValidationFixtures() {
    }

    /** @return the conformant message, freshly parsed so tests cannot affect one another. */
    public static Pacs008Message validMessage() {
        return UNMARSHALLER.unmarshal(PARSER.parse(SampleMessages.pacs008Valid()));
    }

    /**
     * Parses the conformant message after applying a textual substitution.
     *
     * @param target      the exact text to replace
     * @param replacement what to replace it with
     */
    public static Pacs008Message messageWith(String target, String replacement) {
        String mutated = SampleMessages.pacs008ValidAsText().replace(target, replacement);
        return UNMARSHALLER.unmarshal(PARSER.parse(mutated.getBytes(StandardCharsets.UTF_8)));
    }

    /** Parses arbitrary message text into the bound model. */
    public static Pacs008Message parse(String xml) {
        return UNMARSHALLER.unmarshal(PARSER.parse(xml.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * The conformant message with its single transaction repeated verbatim.
     *
     * <p>Used to prove that a transaction identifier repeated inside one batch is caught.
     * Such a repeat never reaches the database, so a repository query alone cannot see it.
     */
    public static String duplicatedTransactionMessage() {
        String xml = SampleMessages.pacs008ValidAsText();
        int start = xml.indexOf("<CdtTrfTxInf>");
        int end = xml.indexOf("</CdtTrfTxInf>") + "</CdtTrfTxInf>".length();
        String transactionBlock = xml.substring(start, end);
        return xml.substring(0, end) + System.lineSeparator() + transactionBlock
                + xml.substring(end);
    }

    /**
     * The conformant message with the ultimate creditor removed.
     *
     * <p>The supplied sample carries the same organisation identifier on both ultimate
     * parties, so both sides resolve to one customer. Removing the creditor side leaves a
     * message in which exactly one party is identified, which is what tests about customer
     * naming need in order to assert anything definite.
     */
    public static String withoutUltimateCreditor() {
        String xml = SampleMessages.pacs008ValidAsText();
        int start = xml.indexOf("<UltmtCdtr>");
        int end = xml.indexOf("</UltmtCdtr>") + "</UltmtCdtr>".length();
        return xml.substring(0, start) + xml.substring(end);
    }

    /** @return a context around the conformant message. */
    public static ValidationContext validContext() {
        return contextOf(validMessage());
    }

    /** @return a context around the given message, with the declared sender matching it. */
    public static ValidationContext contextOf(Pacs008Message message) {
        return new ValidationContext(message, SENDER_BIC, CORRELATION_ID);
    }

    public static Institution institutionA() {
        return new Institution("BANKA000", "Institution A", "FI", true);
    }

    public static Institution institutionB() {
        return new Institution("BANKB000", "Institution B", "RI", true);
    }

    public static Institution clearingSystem() {
        return new Institution("CBANK0IPS", "Central Bank Instant Payment System", null, true);
    }
}
