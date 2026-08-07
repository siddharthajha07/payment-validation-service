package com.rbc.paymentvalidation.testsupport;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Loads the message fixtures used by the tests.
 *
 * <p>{@code pacs008-sample.xml} is the pacs.008 supplied with the assessment, copied
 * verbatim rather than retyped. Testing against the real message is the point: a fixture
 * tidied to suit the parser would prove nothing about the traffic this service will
 * actually receive.
 */
public final class SampleMessages {

    private static final String PACS_008_SAMPLE = "samples/pacs008-sample.xml";
    private static final String PACS_008_VALID = "samples/pacs008-valid.xml";

    private SampleMessages() {
    }

    /** @return the supplied pacs.008 sample, exactly as provided. */
    public static byte[] pacs008Sample() {
        return read(PACS_008_SAMPLE);
    }

    /** @return the supplied pacs.008 sample as text, for tests that mutate it. */
    public static String pacs008SampleAsText() {
        return new String(pacs008Sample(), StandardCharsets.UTF_8);
    }

    /**
     * A message that satisfies every business rule, derived from the supplied sample.
     *
     * <p>The supplied sample deliberately does <em>not</em> satisfy them: its accounts
     * carry no institution prefix and its branch transit identifiers are five digits where
     * the assessment specifies three. Rather than bend the rules to fit the sample, this
     * fixture adjusts exactly four values — the two account numbers and the two transit
     * identifiers — and everything else is byte-for-byte the message as provided.
     */
    public static byte[] pacs008Valid() {
        return read(PACS_008_VALID);
    }

    /** @return the conformant fixture as text, for tests that mutate it. */
    public static String pacs008ValidAsText() {
        return new String(pacs008Valid(), StandardCharsets.UTF_8);
    }

    private static byte[] read(String location) {
        try (InputStream stream =
                     SampleMessages.class.getClassLoader().getResourceAsStream(location)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test fixture: " + location);
            }
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
