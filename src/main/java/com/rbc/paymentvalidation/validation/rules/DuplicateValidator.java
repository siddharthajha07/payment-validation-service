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
 * <h2>How this differs from the idempotency key</h2>
 * These are two different protections against two different mistakes, and the distinction
 * is worth being precise about.
 *
 * <p>The {@code X-Idempotency-Key} header handles the <em>transport</em> problem: a client
 * did not receive a response, cannot know whether the request arrived, and retries. That
 * caller should receive the original answer replayed, not a rejection — nothing has gone
 * wrong.
 *
 * <p>This rule handles the <em>business</em> problem: a genuinely new request, with its own
 * idempotency key, carrying a transaction identifier that has been settled before. That is
 * a defect at the sender and must be rejected, because paying it twice would move money
 * twice.
 *
 * <h2>Why the check inside the message matters too</h2>
 * A single batch can repeat an identifier within itself. That never reaches the database,
 * so a repository query alone would not see it; the identifiers are therefore also checked
 * against each other.
 *
 * <h2>Why this is not the last line of defence</h2>
 * This rule reads and then the service writes, and between those two moments a concurrent
 * request may insert the same identifier. The unique constraint on {@code payment.
 * transaction_id} is what makes duplicate detection actually reliable. This rule exists to
 * turn the common case into a courteous ISO rejection instead of a constraint violation;
 * it does not replace the constraint.
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
