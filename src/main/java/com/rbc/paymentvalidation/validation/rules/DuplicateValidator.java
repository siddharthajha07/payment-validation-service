package com.rbc.paymentvalidation.validation.rules;

import com.rbc.paymentvalidation.repository.PaymentRepository;
import com.rbc.paymentvalidation.validation.PaymentValidator;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationContext;
import com.rbc.paymentvalidation.validation.ValidationError;
import com.rbc.paymentvalidation.validation.ValidationResult;
import com.rbc.paymentvalidation.xml.model.pacs008.CreditTransferTransaction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Rejects a payment whose transaction identifier has already been processed.
 *
 * This is not the same thing as the idempotency key. That handles the transport problem: a
 * client that never saw a response retries, and should get the original answer replayed. This
 * handles the business problem: a genuinely new request carrying a transaction that has been
 * settled before, which is a defect at the sender and would move money twice.
 *
 * Identifiers are also checked against each other, since a batch can repeat one internally and
 * that never reaches the database.
 *
 * It is not the last line of defence. This reads and then the service writes, and a concurrent
 * request can insert in between. The unique constraint on payment.transaction_id is what makes
 * duplicate detection reliable; this rule just turns the common case into a polite rejection.
 */
@Component
@Order(80)
public class DuplicateValidator implements PaymentValidator {

    private final PaymentRepository paymentRepository;

    public DuplicateValidator(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public ValidationResult validate(ValidationContext context) {
        List<ValidationError> errors = new ArrayList<>();
        Set<String> seenInThisMessage = new HashSet<>();
        List<CreditTransferTransaction> transactions = context.transactions();

        for (int i = 0; i < transactions.size(); i++) {
            String transactionId = transactions.get(i).getPaymentIdentification()
                    .getTransactionIdentification();
            String path = "CdtTrfTxInf[%d]/PmtId/TxId".formatted(i);

            if (!seenInThisMessage.add(transactionId)) {
                errors.add(new ValidationError(RejectReasonCode.AM05,
                        "Transaction identifier %s appears more than once in this message"
                                .formatted(transactionId), path));
                continue;
            }

            if (paymentRepository.existsByTransactionId(transactionId)) {
                errors.add(new ValidationError(RejectReasonCode.AM05,
                        "Transaction identifier %s has already been processed"
                                .formatted(transactionId), path));
            }
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.rejected(errors);
    }
}
