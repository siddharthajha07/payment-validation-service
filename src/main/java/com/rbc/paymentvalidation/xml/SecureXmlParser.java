package com.rbc.paymentvalidation.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Parses an inbound payload into a DOM document with external entity processing disabled.
 *
 * <h2>The attack being prevented</h2>
 * XML permits a document to declare entities that expand into other content, including
 * the contents of a local file or a URL:
 * <pre>{@code
 * <!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
 * <foo>&xxe;</foo>
 * }</pre>
 * A default-configured parser resolves that entity, disclosing the file to whoever can
 * see the response or the error. Aimed at an internal URL instead, the same technique
 * lets an external caller probe systems behind the firewall. A related attack nests
 * entities so that they expand exponentially, exhausting memory — the "billion laughs".
 *
 * <h2>The defence</h2>
 * Both attacks require a {@code <!DOCTYPE>} declaration, so refusing to accept one at all
 * defeats both outright. That single feature does most of the work here; the remaining
 * settings are defence in depth in case the parser implementation is ever swapped for one
 * that treats the first feature differently.
 *
 * <h2>Why the factory is built per call</h2>
 * A Spring {@code @Component} is a singleton shared by every request thread, and
 * {@link DocumentBuilderFactory} is not thread-safe. Building a configured factory inside
 * the parse call keeps the hardening free of shared mutable state; the cost is negligible
 * beside parsing itself, and it removes a whole class of concurrency bug.
 */
@Component
public class SecureXmlParser {

    private static final Logger log = LoggerFactory.getLogger(SecureXmlParser.class);

    private static final String DISALLOW_DOCTYPE =
            "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";
    private static final String LOAD_EXTERNAL_DTD =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private final int maxPayloadBytes;

    public SecureXmlParser(@Value("${payment.xml.max-payload-bytes}") int maxPayloadBytes) {
        this.maxPayloadBytes = maxPayloadBytes;
    }

    /**
     * @param payload the raw request body
     * @return the parsed, namespace-aware DOM document
     * @throws XmlProcessingException if the payload is empty, oversized, not well formed,
     *                                or contains a document type declaration
     */
    public Document parse(byte[] payload) {
        if (payload == null || payload.length == 0) {
            throw new XmlProcessingException("Request body is empty");
        }
        if (payload.length > maxPayloadBytes) {
            // Rejected before parsing: an oversized body should never be expanded in memory.
            throw new XmlProcessingException(
                    "Request body of %d bytes exceeds the maximum of %d bytes"
                            .formatted(payload.length, maxPayloadBytes));
        }

        try {
            DocumentBuilder builder = hardenedFactory().newDocumentBuilder();
            builder.setErrorHandler(new FailFastErrorHandler());
            // The parsed document is returned exactly as it was read. In particular it is
            // NOT normalised: Document.normalizeDocument() merges text nodes and performs
            // namespace fixup, both of which change the canonical byte form of the
            // document. A digital signature is computed over exactly those bytes, so
            // normalising a signed message silently makes its signature stop verifying
            // while leaving the XML looking entirely correct.
            return builder.parse(new ByteArrayInputStream(payload));
        } catch (ParserConfigurationException e) {
            throw new XmlProcessingException("XML parser could not be configured securely", e);
        } catch (SAXException | IOException e) {
            // The payload itself is never logged: it carries customer names and account
            // numbers. Size and the parser's own message are enough to diagnose a fault.
            log.warn("Rejected malformed XML payload of {} bytes: {}",
                    payload.length, e.getMessage());
            throw new XmlProcessingException("Request body is not well-formed XML", e);
        }
    }

    private DocumentBuilderFactory hardenedFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // The decisive control: no DOCTYPE means no entity declarations, which defeats
        // both external-entity disclosure and exponential entity expansion.
        factory.setFeature(DISALLOW_DOCTYPE, true);

        // Defence in depth, should the feature above ever be unsupported or bypassed.
        factory.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
        factory.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
        factory.setFeature(LOAD_EXTERNAL_DTD, false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        // Essential, and easy to overlook: without this the parser ignores namespaces and
        // every ISO element would be matched by local name alone, conflating the
        // identically named elements that appear in the header and the document.
        factory.setNamespaceAware(true);

        return factory;
    }

    /**
     * Turns recoverable parse errors into failures. The default handler prints warnings
     * and carries on, which would let a subtly malformed document through.
     */
    private static final class FailFastErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }
    }
}
