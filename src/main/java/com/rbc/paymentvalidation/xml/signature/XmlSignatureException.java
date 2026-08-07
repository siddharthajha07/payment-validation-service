package com.rbc.paymentvalidation.xml.signature;

/**
 * The status report could not be signed.
 *
 * An internal fault, not a business outcome. An unsigned report is not a lesser response that
 * could be sent anyway, since a counterparty cannot act on something it cannot authenticate.
 */
public class XmlSignatureException extends RuntimeException {

    public XmlSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
