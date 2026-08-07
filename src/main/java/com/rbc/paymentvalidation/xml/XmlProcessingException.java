package com.rbc.paymentvalidation.xml;

/**
 * The payload could not be read as XML at all: empty, oversized, malformed, or carrying a
 * DOCTYPE.
 *
 * This is not a business rejection. A message that cannot be parsed cannot be answered with a
 * pacs.002, because a status report has to quote identifiers that were never read. The global
 * handler maps this to 400 with a plain error document instead.
 */
public class XmlProcessingException extends RuntimeException {

    public XmlProcessingException(String message) {
        super(message);
    }

    public XmlProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
