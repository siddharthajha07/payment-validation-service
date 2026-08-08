package com.rbc.paymentvalidation.validation.rules;

import com.rbc.paymentvalidation.validation.PaymentValidator;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationContext;
import com.rbc.paymentvalidation.validation.ValidationError;
import com.rbc.paymentvalidation.validation.ValidationResult;
import com.rbc.paymentvalidation.xml.model.pacs008.ActiveCurrencyAndAmount;
import com.rbc.paymentvalidation.xml.model.pacs008.CreditTransferTransaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Checks the group header's stated totals against what the message actually contains.
 *
 * The sender says how many transactions there are and what they add up to. Checking those
 * claims catches truncation in transit, a batching fault at the sender, and tampering. The
 * cost is one loop; the cost of skipping it is settling a batch that was never complete.
 *
 * The amount comparison uses compareTo rather than equals, because equals compares scale too
 * and would reject a perfectly valid message over a trailing zero.
 */
@Component
@Order(60)
public class ControlTotalsValidator implements PaymentValidator {

    @Override
    public ValidationResult validate(ValidationContext context) {
        List<ValidationError> errors = new ArrayList<>();
        List<CreditTransferTransaction> transactions = context.transactions();

        checkTransactionCount(context.groupHeader().getNumberOfTransactions(),
                transactions.size(), errors);
        checkTotalAmount(context.groupHeader().getTotalInterbankSettlementAmount(),
                transactions, errors);

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.rejected(errors);
    }

    private void checkTransactionCount(String declared, int actual, List<ValidationError> errors) {
        int declaredCount;
        try {
            declaredCount = Integer.parseInt(declared.trim());
        } catch (NumberFormatException e) {
            // The schema constrains NbOfTxs to digits, so reaching here means a value too
            // large for an int rather than a non-numeric one. Either way it cannot match.
            errors.add(new ValidationError(RejectReasonCode.AM18,
                    "Number of transactions is not a usable count", "GrpHdr/NbOfTxs"));
            return;
        }

        if (declaredCount != actual) {
            errors.add(new ValidationError(RejectReasonCode.AM18,
                    "Message declares %d transactions but contains %d"
                            .formatted(declaredCount, actual), "GrpHdr/NbOfTxs"));
        }
    }

    private void checkTotalAmount(ActiveCurrencyAndAmount declaredTotal,
                                  List<CreditTransferTransaction> transactions,
                                  List<ValidationError> errors) {
        if (declaredTotal == null || declaredTotal.getValue() == null) {
            // The total is optional in ISO 20022. Absent, there is no claim to verify.
            return;
        }

        BigDecimal sum = transactions.stream()
                .map(transaction -> transaction.getInterbankSettlementAmount().getValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (declaredTotal.getValue().compareTo(sum) != 0) {
            // The amounts are the sender's own figures, so quoting them back discloses
            // nothing new and is the fastest way for them to see the discrepancy.
            errors.add(new ValidationError(RejectReasonCode.AM09,
                    "Declared total %s does not equal the sum of transactions %s"
                            .formatted(declaredTotal.getValue().toPlainString(),
                                    sum.toPlainString()),
                    "GrpHdr/TtlIntrBkSttlmAmt"));
        }
    }
}
