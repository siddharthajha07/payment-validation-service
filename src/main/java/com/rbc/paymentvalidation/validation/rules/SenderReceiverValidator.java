package com.rbc.paymentvalidation.validation.rules;

import com.rbc.paymentvalidation.validation.PaymentValidator;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationContext;
import com.rbc.paymentvalidation.validation.ValidationResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Checks who the message says it is from and to.
 *
 * <h2>Sender and receiver must differ</h2>
 * An institution cannot send a payment to itself through a clearing system. Such a message
 * is either a configuration error at the sender or an attempt to have the scheme process
 * something that has no business being there, and in both cases the sender needs to be
 * told rather than have the message quietly processed.
 *
 * <h2>The declared sender must match the message</h2>
 * The caller supplies {@code X-Sender-Institution} on the request and also names a sender
 * in the business header. If those disagree, one of two things is happening: a
 * misconfigured client, or a caller attempting to submit a message on another
 * institution's behalf. Comparing them costs nothing and closes that gap.
 *
 * <p>This is authentication-adjacent but not authentication. A production deployment would
 * establish the caller's identity from a mutual-TLS client certificate and check the header
 * against <em>that</em>, rather than trusting a value the caller also supplied. The check
 * as written catches honest misconfiguration; it does not stop a determined impersonator,
 * and it is documented that way.
 */
@Component
@Order(20)
public class SenderReceiverValidator implements PaymentValidator {

    @Override
    public ValidationResult validate(ValidationContext context) {
        String sender = context.senderBic();
        String receiver = context.receiverBic();

        if (sender.equalsIgnoreCase(receiver)) {
            return ValidationResult.rejected(RejectReasonCode.AGNT,
                    "Sender and receiver institutions must not be identical",
                    "AppHdr/Fr and AppHdr/To");
        }

        String declared = context.declaredSenderBic();
        if (declared != null && !declared.isBlank() && !declared.equalsIgnoreCase(sender)) {
            return ValidationResult.rejected(RejectReasonCode.AGNT,
                    "Declared sender institution does not match the sender in the message",
                    "X-Sender-Institution and AppHdr/Fr");
        }

        return ValidationResult.valid();
    }
}
