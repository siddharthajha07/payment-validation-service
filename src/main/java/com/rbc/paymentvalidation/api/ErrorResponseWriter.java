package com.rbc.paymentvalidation.api;

import com.rbc.paymentvalidation.api.dto.ErrorResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import org.springframework.stereotype.Component;

/**
 * Serialises an ErrorResponse to XML.
 *
 * String concatenation would be shorter and would be a bug waiting to happen. Error documents
 * quote values the caller supplied, and one containing < or & would either produce malformed
 * XML or let the caller inject elements into a document the receiver trusts. A marshaller
 * escapes as a matter of course.
 */
@Component
public class ErrorResponseWriter {

    private final JAXBContext jaxbContext;

    public ErrorResponseWriter() {
        try {
            this.jaxbContext = JAXBContext.newInstance(ErrorResponse.class);
        } catch (JAXBException e) {
            throw new IllegalStateException("Unable to create the error response context", e);
        }
    }

    /**
     * @param errorResponse the error to serialise
     * @return the error as XML, or a minimal fixed document if serialisation itself fails.
     *         An exception raised while reporting an exception must not replace the
     *         original fault with a less informative one.
     */
    public String write(ErrorResponse errorResponse) {
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter writer = new StringWriter();
            marshaller.marshal(errorResponse, writer);
            return writer.toString();
        } catch (JAXBException e) {
            return """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <ErrorResponse><Code>INTERNAL_ERROR</Code></ErrorResponse>""";
        }
    }
}
