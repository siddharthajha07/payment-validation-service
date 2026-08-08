package com.rbc.paymentvalidation.validation;

import com.rbc.paymentvalidation.xml.model.envelope.Pacs008Message;
import com.rbc.paymentvalidation.xml.model.header.BusinessApplicationHeader;
import com.rbc.paymentvalidation.xml.model.pacs008.CreditTransferTransaction;
import com.rbc.paymentvalidation.xml.model.pacs008.GroupHeader;
import java.util.List;

/**
 * Everything a rule needs to reach a decision.
 *
 * Rules get this rather than the raw message so request-level facts are available without any
 * rule reaching back into the HTTP layer. That keeps them testable as plain objects.
 *
 * The accessors tolerate missing structure and return null or an empty list. A rule's job is
 * to report an absent element as a rejection with a proper reason code, not to throw a
 * NullPointerException the sender sees as an internal error.
 */
public record ValidationContext(Pacs008Message message, String declaredSenderBic,
                                String correlationId) {

    /** @return the business application header, or null if absent. */
    public BusinessApplicationHeader header() {
        return message == null ? null : message.getApplicationHeader();
    }

    /** @return the group header, or null if absent. */
    public GroupHeader groupHeader() {
        if (message == null || message.getCreditTransfer() == null) {
            return null;
        }
        return message.getCreditTransfer().getGroupHeader();
    }

    /** @return the transactions in the message, never null. */
    public List<CreditTransferTransaction> transactions() {
        if (message == null || message.getCreditTransfer() == null) {
            return List.of();
        }
        return message.getCreditTransfer().getCreditTransferTransactions();
    }

    /** @return the sender BIC from the business header, or null if absent. */
    public String senderBic() {
        return header() == null ? null : header().senderBic();
    }

    /** @return the receiver BIC from the business header, or null if absent. */
    public String receiverBic() {
        return header() == null ? null : header().receiverBic();
    }
}
