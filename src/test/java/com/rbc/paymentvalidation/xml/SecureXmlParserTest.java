package com.rbc.paymentvalidation.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rbc.paymentvalidation.testsupport.SampleMessages;
import com.rbc.paymentvalidation.xml.model.IsoNamespaces;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Tests for the hardened XML parser.
 *
 * <p>The attack cases carry most of the weight here: they demonstrate that the protection
 * required by the specification is real and enforced, rather than asserted in a comment.
 */
class SecureXmlParserTest {

    private static final int ONE_MEGABYTE = 1_048_576;

    private final SecureXmlParser parser = new SecureXmlParser(ONE_MEGABYTE);

    @Nested
    @DisplayName("Attack payloads")
    class AttackPayloads {

        @Test
        @DisplayName("rejects an external entity referencing a local file")
        void rejectsExternalEntityReferencingLocalFile() {
            // The classic XXE: without hardening the parser reads /etc/passwd and splices
            // its contents into the document, disclosing them to the caller.
            String payload = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <!DOCTYPE message [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
                    <message>&xxe;</message>
                    """;

            assertThatThrownBy(() -> parser.parse(payload.getBytes(StandardCharsets.UTF_8)))
                    .isInstanceOf(XmlProcessingException.class);
        }

        @Test
        @DisplayName("rejects an external entity referencing a remote URL")
        void rejectsExternalEntityReferencingRemoteUrl() {
            // The same technique aimed outward: a parser that resolves this becomes a way
            // to reach hosts the caller cannot reach directly.
            String payload = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <!DOCTYPE message [
                      <!ENTITY xxe SYSTEM "http://internal-host.invalid/secret">
                    ]>
                    <message>&xxe;</message>
                    """;

            assertThatThrownBy(() -> parser.parse(payload.getBytes(StandardCharsets.UTF_8)))
                    .isInstanceOf(XmlProcessingException.class);
        }

        @Test
        @DisplayName("rejects an external document type declaration")
        void rejectsExternalDocumentTypeDeclaration() {
            String payload = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <!DOCTYPE message SYSTEM "http://internal-host.invalid/evil.dtd">
                    <message>text</message>
                    """;

            assertThatThrownBy(() -> parser.parse(payload.getBytes(StandardCharsets.UTF_8)))
                    .isInstanceOf(XmlProcessingException.class);
        }

        @Test
        @DisplayName("rejects exponential entity expansion, the billion laughs attack")
        void rejectsExponentialEntityExpansion() {
            // Each entity references the previous one ten times. Fully expanded this is
            // hundreds of megabytes from a payload of a few hundred bytes. Refusing the
            // document type declaration stops it before any expansion is attempted.
            String payload = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <!DOCTYPE lolz [
                      <!ENTITY lol "lol">
                      <!ENTITY lol1 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
                      <!ENTITY lol2 "&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;">
                      <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
                      <!ENTITY lol4 "&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;">
                      <!ENTITY lol5 "&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;">
                      <!ENTITY lol6 "&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;">
                    ]>
                    <lolz>&lol6;</lolz>
                    """;

            assertThatThrownBy(() -> parser.parse(payload.getBytes(StandardCharsets.UTF_8)))
                    .isInstanceOf(XmlProcessingException.class);
        }
    }

    @Nested
    @DisplayName("Malformed input")
    class MalformedInput {

        @Test
        @DisplayName("rejects an empty body")
        void rejectsEmptyBody() {
            assertThatThrownBy(() -> parser.parse(new byte[0]))
                    .isInstanceOf(XmlProcessingException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("rejects a null body")
        void rejectsNullBody() {
            assertThatThrownBy(() -> parser.parse(null))
                    .isInstanceOf(XmlProcessingException.class);
        }

        @Test
        @DisplayName("rejects XML that is not well formed")
        void rejectsXmlThatIsNotWellFormed() {
            byte[] payload = "<message><unclosed></message>".getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> parser.parse(payload))
                    .isInstanceOf(XmlProcessingException.class)
                    .hasMessageContaining("not well-formed");
        }

        @Test
        @DisplayName("rejects a body larger than the configured maximum before parsing it")
        void rejectsOversizedBody() {
            SecureXmlParser tinyLimitParser = new SecureXmlParser(64);
            byte[] payload = SampleMessages.pacs008Sample();

            assertThatThrownBy(() -> tinyLimitParser.parse(payload))
                    .isInstanceOf(XmlProcessingException.class)
                    .hasMessageContaining("exceeds the maximum");
        }
    }

    @Nested
    @DisplayName("Valid input")
    class ValidInput {

        @Test
        @DisplayName("parses the supplied pacs.008 sample")
        void parsesTheSuppliedSample() {
            Document document = parser.parse(SampleMessages.pacs008Sample());

            assertThat(document).isNotNull();
            assertThat(document.getDocumentElement().getLocalName()).isEqualTo("Message");
        }

        @Test
        @DisplayName("is namespace aware, so identically named elements stay distinguishable")
        void isNamespaceAware() {
            // Without namespace awareness every ISO element would be matched by local name
            // alone, conflating BICFI in the header with BICFI in the document.
            Document document = parser.parse(SampleMessages.pacs008Sample());

            assertThat(document.getDocumentElement().getNamespaceURI())
                    .isEqualTo(IsoNamespaces.MONTRAN_ENVELOPE);
            // The sample carries five BICFI elements in total: the sender and receiver in
            // the header, and the instructing, debtor and creditor agents in the document.
            assertThat(document.getElementsByTagNameNS(IsoNamespaces.HEAD_001_001_03, "BICFI")
                    .getLength()).isEqualTo(2);
            assertThat(document.getElementsByTagNameNS(IsoNamespaces.PACS_008_001_12, "BICFI")
                    .getLength()).isEqualTo(3);
        }
    }
}
