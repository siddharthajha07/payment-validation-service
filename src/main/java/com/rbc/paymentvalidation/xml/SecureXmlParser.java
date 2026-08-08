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
 * Parses an inbound payload into a DOM with external entity resolution turned off.
 *
 * XML lets a document declare entities that expand into other content, including a local
 * file or a URL. A default parser resolves them, which leaks files to whoever sees the
 * response and lets an outside caller reach hosts behind the firewall. A related trick
 * nests entities so they expand exponentially and exhaust memory.
 *
 * Both need a DOCTYPE declaration, so refusing one outright defeats both. The remaining
 * settings are belt and braces in case the parser implementation is ever swapped.
 *
 * The factory is rebuilt per call because this bean is a singleton shared by request
 * threads and DocumentBuilderFactory is not thread-safe.
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

    /** Throws XmlProcessingException if the payload is empty, oversized, malformed or has a DOCTYPE. */
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
            // Returned exactly as read. Deliberately not normalised: normalizeDocument()
            // merges text nodes and rewrites namespaces, which changes the canonical bytes
            // a signature was computed over and silently breaks verification.
            return builder.parse(new ByteArrayInputStream(payload));
        } catch (ParserConfigurationException e) {
            throw new XmlProcessingException("XML parser could not be configured securely", e);
        } catch (SAXException | IOException e) {
            // Never log the payload itself; it carries names and account numbers.
            log.warn("Rejected malformed XML payload of {} bytes: {}",
                    payload.length, e.getMessage());
            throw new XmlProcessingException("Request body is not well-formed XML", e);
        }
    }

    private DocumentBuilderFactory hardenedFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // The one that matters: no DOCTYPE means no entity declarations at all.
        factory.setFeature(DISALLOW_DOCTYPE, true);

        // Belt and braces if the above is ever unsupported.
        factory.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
        factory.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
        factory.setFeature(LOAD_EXTERNAL_DTD, false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        // Easy to overlook. Without it the parser matches on local name alone, so BICFI in
        // the header and BICFI in the document become the same element.
        factory.setNamespaceAware(true);

        return factory;
    }

    /** The default handler prints warnings and carries on, which would let bad XML through. */
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
