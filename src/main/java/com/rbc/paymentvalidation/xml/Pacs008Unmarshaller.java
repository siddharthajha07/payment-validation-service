package com.rbc.paymentvalidation.xml;

import com.rbc.paymentvalidation.xml.model.envelope.Pacs008Message;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

/**
 * Turns a validated document into the Pacs008Message object graph.
 *
 * Unmarshalling works from the DOM the secure parser already produced. Handing the raw bytes
 * to JAXB would parse them a second time with a parser we have not hardened, which would put
 * back the external entity hole SecureXmlParser exists to close.
 *
 * JAXBContext is immutable and expensive, so one is built at startup; Unmarshaller is neither,
 * so one is made per message.
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

    public Pacs008Message unmarshal(Document document) {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (Pacs008Message) unmarshaller.unmarshal(document);
        } catch (JAXBException e) {
            throw new XmlProcessingException("Message could not be bound to the pacs.008 model", e);
        }
    }
}
