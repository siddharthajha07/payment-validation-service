package com.rbc.paymentvalidation.validation;

import com.rbc.paymentvalidation.xml.model.envelope.Pacs008Message;
import com.rbc.paymentvalidation.xml.model.header.BusinessApplicationHeader;
import com.rbc.paymentvalidation.xml.model.pacs008.CreditTransferTransaction;
import com.rbc.paymentvalidation.xml.model.pacs008.GroupHeader;
import java.util.List;

/**
 * Everything a rule needs in order to reach a decision.
 *
 * <p>Rules receive this rather than the raw message so that request-level facts — the
 * declared sender, the correlation id — are available to them without any rule reaching
 * back into the HTTP layer. That keeps the rules free of any dependency on how the message
 * arrived, which is what makes them testable as plain objects with no framework running.
 *
 * <p>The accessors below tolerate absent structure and return {@code null} or an empty
 * list rather than throwing. A rule's job is to report a missing element as a rejection
 * with a proper reason code, not to fail with a {@code NullPointerException} that the
 * sender would see as an opaque internal error.
 *
 * @param message                 the bound pacs.008
 * @param declaredSenderBic       the {@code X-Sender-Institution} header supplied by the caller
 * @param correlationId           the identifier tying together every log line and audit
 *                                event for this request
 */
public record ValidationContext(Pacs008Message message, String declaredSenderBic,
                                String correlationId) {

    /** @return the business application header, or {@code null} if absent. */
    public BusinessApplicationHeader header() {
        return message == null ? null : message.getApplicationHeader();
    }

    /** @return the group header, or {@code null} if absent. */
    public GroupHeader groupHeader() {
        if (message == null || message.getCreditTransfer() == null) {
            return null;
        }
        return message.getCreditTransfer().getGroupHeader();
    }

    /** @return the transactions in the message, never {@code null}. */
    public List<CreditTransferTransaction> transactions() {
        if (message == null || message.getCreditTransfer() == null) {
            return List.of();
        }
        return message.getCreditTransfer().getCreditTransferTransactions();
    }

    /** @return the sender BIC from the business header, or {@code null} if absent. */
    public String senderBic() {
        return header() == null ? null : header().senderBic();
    }

    /** @return the receiver BIC from the business header, or {@code null} if absent. */
    public String receiverBic() {
        return header() == null ? null : header().receiverBic();
    }
}
