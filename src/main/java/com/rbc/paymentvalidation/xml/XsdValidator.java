package com.rbc.paymentvalidation.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Validates a parsed document against the inbound message schema.
 *
 * <h2>Where schema validation sits</h2>
 * Schema validation answers a structural question — are the required elements present,
 * in the right order, with values in the right lexical form. It deliberately does not
 * answer business questions such as whether the amount is positive or the currency is
 * supported. Those are enforced later by the validator chain, so that a message which is
 * understood but unacceptable receives a signed pacs.002 rejection carrying an ISO reason
 * code, while a message that cannot be understood receives a plain HTTP 400.
 *
 * <h2>How external access is prevented</h2>
 * The three schema documents are read from the classpath at startup and compiled into a
 * single {@link Schema}. The envelope schema imports the other two by namespace only,
 * with no {@code schemaLocation} hint, so nothing is ever fetched: external DTD and
 * schema access are both set to the empty string, which forbids every protocol. An
 * attacker-supplied {@code schemaLocation} in a payload therefore has nothing to act on.
 *
 * <h2>Thread safety</h2>
 * A compiled {@link Schema} is immutable and safe to share, so it is built once. A
 * {@link Validator} is not, so a fresh one is obtained per call — cheap, since it reuses
 * the already-compiled schema.
 */
@Component
public class XsdValidator {

    private static final Logger log = LoggerFactory.getLogger(XsdValidator.class);

    /**
     * Imported schemas are listed before the schema that imports them, so that every
     * namespace is already known by the time the envelope references it.
     */
    private static final List<String> SCHEMA_RESOURCES = List.of(
            "xsd/head.001.001.03-subset.xsd",
            "xsd/pacs.008.001.12-subset.xsd",
            "xsd/montran-pacs008-envelope.xsd");

    private final Schema schema;

    public XsdValidator() {
        this.schema = compileSchema();
    }

    /**
     * @param document a parsed inbound message
     * @throws SchemaValidationException listing every violation found, if any
     */
    public void validate(Document document) {
        CollectingErrorHandler errorHandler = new CollectingErrorHandler();
        try {
            Validator validator = schema.newValidator();
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            validator.setErrorHandler(errorHandler);
            validator.validate(new DOMSource(document));
        } catch (SAXException | IOException e) {
            throw new SchemaValidationException(
                    "Message could not be validated against the schema",
                    errorHandler.violations.isEmpty()
                            ? List.of(e.getMessage())
                            : errorHandler.violations);
        }

        if (!errorHandler.violations.isEmpty()) {
            // Violations name elements and constraints, never element content, so they are
            // safe to log and to return to the sender.
            log.warn("Message failed schema validation with {} violation(s): {}",
                    errorHandler.violations.size(), errorHandler.violations);
            throw new SchemaValidationException(
                    "Message does not conform to the pacs.008 schema",
                    errorHandler.violations);
        }
    }

    private Schema compileSchema() {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Source[] sources = new Source[SCHEMA_RESOURCES.size()];
            for (int i = 0; i < SCHEMA_RESOURCES.size(); i++) {
                String location = SCHEMA_RESOURCES.get(i);
                InputStream stream = new ClassPathResource(location).getInputStream();
                sources[i] = new StreamSource(stream, location);
            }
            return factory.newSchema(sources);
        } catch (SAXException | IOException e) {
            // Failing at startup is intended: a service that cannot compile its own
            // schemas cannot validate anything, and should not accept traffic.
            throw new IllegalStateException("Unable to compile message schemas", e);
        }
    }

    /** Gathers every violation instead of stopping at the first. */
    private static final class CollectingErrorHandler implements ErrorHandler {

        private final List<String> violations = new ArrayList<>();

        @Override
        public void warning(SAXParseException e) {
            // Warnings are advisory and do not make the message unusable.
        }

        @Override
        public void error(SAXParseException e) {
            violations.add(describe(e));
        }

        @Override
        public void fatalError(SAXParseException e) {
            violations.add(describe(e));
        }

        private String describe(SAXParseException e) {
            return "line %d, column %d: %s".formatted(
                    e.getLineNumber(), e.getColumnNumber(), e.getMessage());
        }
    }
}
