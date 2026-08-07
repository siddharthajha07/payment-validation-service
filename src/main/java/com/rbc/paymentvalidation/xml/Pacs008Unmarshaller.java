package com.rbc.paymentvalidation.xml;

import com.rbc.paymentvalidation.xml.model.envelope.Pacs008Message;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 * Converts a validated DOM document into the {@link Pacs008Message} object graph.
 *
 * <h2>Why unmarshalling happens from a DOM rather than from the raw bytes</h2>
 * The payload is parsed once, by {@link SecureXmlParser}, and the resulting document is
 * then validated and unmarshalled. Handing the raw bytes to JAXB instead would mean
 * parsing a second time with a parser this class does not control — and therefore has not
 * hardened — which would reopen the external-entity hole that {@code SecureXmlParser}
 * closes. Parsing once, securely, and reusing the result is both safer and faster.
 *
 * <h2>Thread safety</h2>
 * A {@link JAXBContext} is immutable, safe to share, and expensive to build, so one is
 * created at startup. An {@link Unmarshaller} is neither immutable nor thread-safe, so a
 * fresh one is created per message; that is cheap once the context exists.
 */
@Component
public class Pacs008Unmarshaller {

    private final JAXBContext jaxbContext;

    public Pacs008Unmarshaller() {
        try {
            this.jaxbContext = JAXBContext.newInstance(Pacs008Message.class);
        } catch (JAXBException e) {
            // A binding that cannot be built is a defect in the model, not a runtime
            // condition, so the service fails to start rather than failing per request.
            throw new IllegalStateException("Unable to create the pacs.008 JAXB context", e);
        }
    }

    /**
     * @param document a document already validated against the pacs.008 schema
     * @return the bound message
     * @throws XmlProcessingException if the document cannot be bound to the model
     */
    public Pacs008Message unmarshal(Document document) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (Pacs008Message) unmarshaller.unmarshal(document);
        } catch (JAXBException e) {
            throw new XmlProcessingException("Message could not be bound to the pacs.008 model", e);
        }
    }
}
