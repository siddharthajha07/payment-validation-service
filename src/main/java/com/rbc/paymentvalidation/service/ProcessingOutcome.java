package com.rbc.paymentvalidation.service;

/**
 * What the service decided about a request, and what to send back.
 *
 * <p>The HTTP status travels alongside the response body rather than being derived from it
 * by the controller. The two are decided together — an acceptance is a 200 with an
 * {@code ACCP} report, a rejection a 422 with a signed {@code RJCT} report — and keeping
 * them together means the controller does not have to re-derive the outcome by inspecting
 * XML it has just been handed.
 *
 * @param httpStatus    the status to return
 * @param responseXml   the signed status report
 * @param replay        whether this response was replayed from a previous identical request
 * @param transactionId the transaction the response concerns, or {@code null}
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
