package com.rbc.paymentvalidation.service;

/**
 * What the service decided, and what to send back.
 *
 * The status travels with the body rather than being derived from it, because the two are
 * decided together and the controller should not have to re-read XML it was just handed.
 */
public record ProcessingOutcome(int httpStatus, String responseXml, boolean replay,
                                String transactionId) {

    public static ProcessingOutcome accepted(String responseXml, String transactionId) {
        return new ProcessingOutcome(200, responseXml, false, transactionId);
    }

    public static ProcessingOutcome rejected(String responseXml, String transactionId) {
        // 422 Unprocessable Content: the request was well formed and understood, but the
        // instruction it carried cannot be acted on. A 400 would suggest the message was
        // malformed, which is a different fault with a different fix.
        return new ProcessingOutcome(422, responseXml, false, transactionId);
    }

    public static ProcessingOutcome replayed(String responseXml, int originalStatus,
                                             String transactionId) {
        // A replay returns the original status as well as the original body: a retry should
        // be indistinguishable from the exchange it repeats.
        return new ProcessingOutcome(originalStatus, responseXml, true, transactionId);
    }
}
