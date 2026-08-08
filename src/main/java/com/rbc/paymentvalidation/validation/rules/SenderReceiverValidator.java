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
 * An institution cannot clear a payment to itself, so identical sender and receiver is either
 * a configuration error or an attempt to push something through the scheme that does not
 * belong there. Either way the sender needs telling.
 *
 * The caller also declares a sender in X-Sender-Institution, and comparing that to the header
 * costs nothing. Note this is not authentication: it catches a misconfigured client but not a
 * determined impersonator, since the caller supplies both values. Production would take the
 * identity from a mutual-TLS client certificate and check the header against that.
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
