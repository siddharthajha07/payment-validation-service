package com.rbc.paymentvalidation.xml;

/**
 * Raised when an inbound payload cannot be read as XML at all — it is empty, oversized,
 * not well formed, or contains constructs the hardened parser refuses.
 *
 * <p>This is distinct from a business rejection. A message that cannot be parsed cannot
 * be answered with a pacs.002, because a status report has to quote the identifiers of
 * the message it refers to and those identifiers could not be read. The global exception
 * handler therefore maps this to HTTP 400 with a plain error document, whereas a message
 * that parses but breaks a business rule receives a signed pacs.002 rejection.
 */
public class XmlProcessingException extends RuntimeException {

    public XmlProcessingException(String message) {
        super(message);
    }

    public XmlProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
