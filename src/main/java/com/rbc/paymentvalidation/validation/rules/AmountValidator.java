package com.rbc.paymentvalidation.validation.rules;

import com.rbc.paymentvalidation.validation.PaymentValidator;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationContext;
import com.rbc.paymentvalidation.validation.ValidationError;
import com.rbc.paymentvalidation.validation.ValidationProperties;
import com.rbc.paymentvalidation.validation.ValidationResult;
import com.rbc.paymentvalidation.xml.model.pacs008.ActiveCurrencyAndAmount;
import com.rbc.paymentvalidation.xml.model.pacs008.CreditTransferTransaction;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Checks the settlement amount and currency of every transaction.
 *
 * <h2>Three separate faults, three separate codes</h2>
 * A zero amount, a negative amount and an over-precise amount are different mistakes and
 * receive different ISO codes ({@code AM01}, {@code AM02}, {@code AM02} respectively), so
 * that the sender's software can tell them apart without reading English text.
 *
 * <h2>Why precision is checked at all</h2>
 * The schema permits any decimal. A payment for 1.005 in a currency with two decimal
 * places cannot be settled: someone downstream would have to round it, and rounding
 * someone else's money silently is exactly the sort of thing that must not happen. Better
 * to reject it and have the sender state what they meant.
 *
 * <p>Trailing zeros are normalised away before the check, so {@code 1.020} is treated as
 * two decimal places rather than three. A trailing zero adds no precision and rejecting it
 * would be pedantry rather than protection.
 */
@Component
@Order(50)
public class AmountValidator implements PaymentValidator {

    private final ValidationProperties properties;

    public AmountValidator(ValidationProperties properties) {
        this.properties = properties;
    }

    @Override
    public ValidationResult validate(ValidationContext context) {
        List<ValidationError> errors = new ArrayList<>();
        List<CreditTransferTransaction> transactions = context.transactions();

        for (int i = 0; i < transactions.size(); i++) {
            ActiveCurrencyAndAmount amount = transactions.get(i).getInterbankSettlementAmount();
            String path = "CdtTrfTxInf[%d]/IntrBkSttlmAmt".formatted(i);

            checkValue(amount.getValue(), path, errors);
            checkCurrency(amount.getCurrency(), path + "/@Ccy", errors);
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.rejected(errors);
    }

    private void checkValue(BigDecimal value, String path, List<ValidationError> errors) {
        int comparedToZero = value.compareTo(BigDecimal.ZERO);

        if (comparedToZero == 0) {
            errors.add(new ValidationError(RejectReasonCode.AM01,
                    "Settlement amount must be greater than zero", path));
            return;
        }
        if (comparedToZero < 0) {
            errors.add(new ValidationError(RejectReasonCode.AM02,
                    "Settlement amount must not be negative", path));
            return;
        }

        // stripTrailingZeros first: 1.020 carries no more precision than 1.02, and the
        // scale of a stripped value can be negative for large round numbers such as 1E+2.
        int scale = Math.max(value.stripTrailingZeros().scale(), 0);
        if (scale > properties.maxFractionDigits()) {
            errors.add(new ValidationError(RejectReasonCode.AM02,
                    "Settlement amount must have at most %d decimal places"
                            .formatted(properties.maxFractionDigits()), path));
        }
    }

    private void checkCurrency(String currency, String path, List<ValidationError> errors) {
        if (!properties.supportedCurrency().equals(currency)) {
            errors.add(new ValidationError(RejectReasonCode.AM03,
                    "Currency %s is not settled by this service; expected %s"
                            .formatted(currency, properties.supportedCurrency()), path));
        }
    }
}
