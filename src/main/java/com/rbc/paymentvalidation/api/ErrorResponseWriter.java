package com.rbc.paymentvalidation.api;

import com.rbc.paymentvalidation.api.dto.ErrorResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import org.springframework.stereotype.Component;

/**
 * Serialises an {@link ErrorResponse} to XML.
 *
 * <h2>Why this exists rather than building the XML by hand</h2>
 * String concatenation would be shorter and would be a defect waiting to happen. Error
 * documents quote values that came from the caller, and a value containing {@code <} or
 * {@code &} would either produce malformed XML or, worse, allow the caller to inject
 * elements into a document the receiving system trusts. A marshaller escapes content as a
 * matter of course; a template does so only when its author remembers to.
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
