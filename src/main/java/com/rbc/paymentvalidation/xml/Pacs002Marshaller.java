package com.rbc.paymentvalidation.xml;

import com.rbc.paymentvalidation.xml.model.IsoNamespaces;
import com.rbc.paymentvalidation.xml.model.envelope.Pacs002Message;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.glassfish.jaxb.runtime.marshaller.NamespacePrefixMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 * Turns a status report into XML.
 *
 * Marshalling produces a DOM because the signature has to be inserted into the document and
 * computed over its canonical form. toXml deliberately does not indent: a signature covers
 * exact bytes, so adding whitespace afterwards breaks it while leaving the XML looking fine.
 */
@Component
public class Pacs002Marshaller {

    private static final Logger log = LoggerFactory.getLogger(Pacs002Marshaller.class);

    /** Property recognised by the JAXB reference implementation for prefix control. */
    private static final String PREFIX_MAPPER_PROPERTY =
            "org.glassfish.jaxb.namespacePrefixMapper";

    private final JAXBContext jaxbContext;

    public Pacs002Marshaller() {
        try {
            this.jaxbContext = JAXBContext.newInstance(Pacs002Message.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create the pacs.002 JAXB context", e);
        }
    }

    public Document toDocument(Pacs002Message message) {
        try {
            Document document = newDocument();
            Marshaller marshaller = jaxbContext.createMarshaller();
            applyReadablePrefixes(marshaller);
            marshaller.marshal(message, document);
            return document;
        } catch (JAXBException | ParserConfigurationException e) {
            throw new XmlProcessingException("Unable to marshal the status report", e);
        }
    }

    public String toXml(Document document) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            // Intentionally no INDENT: see the class comment. Indenting a signed document
            // changes the bytes the signature was computed over.

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new XmlProcessingException("Unable to serialise the status report", e);
        }
    }

    private Document newDocument() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // Essential: without namespace awareness the marshalled document would carry no
        // namespace information and the signature would be computed over the wrong thing.
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().newDocument();
    }

    /**
     * Asks the marshaller to emit the prefixes used in the supplied samples.
     *
     * Purely cosmetic — prefixes are arbitrary labels and a receiver matches on the
     * namespace URI — but output that resembles the samples is easier to compare against
     * them by eye. The property is specific to the JAXB reference implementation, so an
     * unsupported property is ignored rather than allowed to fail a response.
     */
    private void applyReadablePrefixes(Marshaller marshaller) {
        try {
            marshaller.setProperty(PREFIX_MAPPER_PROPERTY, new NamespacePrefixMapper() {
                @Override
                public String getPreferredPrefix(String namespaceUri, String suggestion,
                                                 boolean requirePrefix) {
                    return switch (namespaceUri) {
                        case IsoNamespaces.MONTRAN_ENVELOPE -> "env";
                        case IsoNamespaces.HEAD_001_001_03 -> "head";
                        case IsoNamespaces.PACS_002_001_14 -> "pacs";
                        default -> suggestion;
                    };
                }
            });
        } catch (JAXBException e) {
            log.debug("Namespace prefix mapping unavailable; default prefixes will be used");
        }
    }
}
