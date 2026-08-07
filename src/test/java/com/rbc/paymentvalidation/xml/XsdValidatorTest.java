package com.rbc.paymentvalidation.xml;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rbc.paymentvalidation.testsupport.SampleMessages;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Tests for schema validation.
 *
 * Each negative case mutates the supplied sample rather than constructing an artificial
 * document, so what is proved is that a realistic message with one specific fault is
 * caught — not merely that some unrelated invalid document fails.
 */
class XsdValidatorTest {

    private final SecureXmlParser parser = new SecureXmlParser(1_048_576);
    private final XsdValidator validator = new XsdValidator();

    private Document parse(String xml) {
        return parser.parse(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("accepts the supplied pacs.008 sample unchanged")
    void acceptsTheSuppliedSample() {
        Document document = parser.parse(SampleMessages.pacs008Sample());

        assertThatCode(() -> validator.validate(document)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rejects a message missing a mandatory header element")
    void rejectsMessageMissingMandatoryHeaderElement() {
        String mutated = SampleMessages.pacs008SampleAsText()
                .replace("<BizMsgIdr>MIB621200494113</BizMsgIdr>", "");

        assertThatThrownBy(() -> validator.validate(parse(mutated)))
                .isInstanceOf(SchemaValidationException.class)
                .hasMessageContaining("does not conform");
    }

    @Test
    @DisplayName("rejects a message missing a mandatory document element")
    void rejectsMessageMissingMandatoryDocumentElement() {
        String mutated = SampleMessages.pacs008SampleAsText()
                .replace("<MsgId>MIB621200494113</MsgId>", "");

        assertThatThrownBy(() -> validator.validate(parse(mutated)))
                .isInstanceOf(SchemaValidationException.class);
    }

    @Test
    @DisplayName("rejects a BIC that is too short to be an institution identifier")
    void rejectsMalformedBic() {
        String mutated = SampleMessages.pacs008SampleAsText()
                .replaceFirst("<BICFI>BANKA000</BICFI>", "<BICFI>SHORT</BICFI>");

        assertThatThrownBy(() -> validator.validate(parse(mutated)))
                .isInstanceOf(SchemaValidationException.class);
    }

    @Test
    @DisplayName("rejects an amount whose currency attribute is not a three-letter code")
    void rejectsMalformedCurrencyCode() {
        String mutated = SampleMessages.pacs008SampleAsText()
                .replace("Ccy=\"BBD\"", "Ccy=\"BARBADOS\"");

        assertThatThrownBy(() -> validator.validate(parse(mutated)))
                .isInstanceOf(SchemaValidationException.class);
    }

    @Test
    @DisplayName("rejects a settlement date that is not a valid date")
    void rejectsMalformedSettlementDate() {
        String mutated = SampleMessages.pacs008SampleAsText()
                .replace("<IntrBkSttlmDt>2026-07-31</IntrBkSttlmDt>",
                        "<IntrBkSttlmDt>31-07-2026</IntrBkSttlmDt>");

        assertThatThrownBy(() -> validator.validate(parse(mutated)))
                .isInstanceOf(SchemaValidationException.class);
    }

    @Test
    @DisplayName("reports every violation found, not only the first")
    void reportsEveryViolation() {
        // A sender correcting a message should learn everything that is wrong in one
        // exchange rather than one fault per round trip.
        String mutated = SampleMessages.pacs008SampleAsText()
                .replace("<BizMsgIdr>MIB621200494113</BizMsgIdr>", "")
                .replace("<MsgId>MIB621200494113</MsgId>", "");

        assertThatThrownBy(() -> validator.validate(parse(mutated)))
                .isInstanceOf(SchemaValidationException.class)
                .satisfies(thrown -> {
                    SchemaValidationException e = (SchemaValidationException) thrown;
                    org.assertj.core.api.Assertions.assertThat(e.getViolations()).hasSize(2);
                });
    }

    @Test
    @DisplayName("rejects a document whose amount is not numeric")
    void rejectsNonNumericAmount() {
        String mutated = SampleMessages.pacs008SampleAsText()
                .replace(">1.02</IntrBkSttlmAmt>", ">not-a-number</IntrBkSttlmAmt>");

        assertThatThrownBy(() -> validator.validate(parse(mutated)))
                .isInstanceOf(SchemaValidationException.class);
    }
}
