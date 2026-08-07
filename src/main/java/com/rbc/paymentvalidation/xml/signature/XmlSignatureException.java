package com.rbc.paymentvalidation.xml.signature;

/**
 * Raised when a status report cannot be signed.
 *
 * <p>This is an internal fault, not a business outcome. An unsigned status report is not a
 * lesser response that could be sent anyway — a counterparty cannot act on a response it
 * cannot authenticate, so failing to sign means failing to answer.
 */
public class XmlSignatureException extends RuntimeException {

    public XmlSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
