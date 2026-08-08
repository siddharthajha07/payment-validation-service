package com.rbc.paymentvalidation.validation.rules;

import com.rbc.paymentvalidation.validation.PaymentValidator;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationContext;
import com.rbc.paymentvalidation.validation.ValidationProperties;
import com.rbc.paymentvalidation.validation.ValidationResult;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Checks the settlement date falls inside an acceptable window.
 *
 * The Clock is injected rather than calling LocalDate.now() directly. A rule about today is
 * untestable otherwise: a test asserting some date is in the future stops being true once that
 * date arrives. With an injected clock a test can fix today and keep meaning what it says.
 *
 * A past date is rejected because settlement cannot be backdated. The date is optional in ISO,
 * and when absent there is nothing to check.
 */
@Component
@Order(70)
public class SettlementDateValidator implements PaymentValidator {

    private final ValidationProperties properties;
    private final Clock clock;

    public SettlementDateValidator(ValidationProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public ValidationResult validate(ValidationContext context) {
        String rawDate = context.groupHeader().getInterbankSettlementDate();
        if (rawDate == null || rawDate.isBlank()) {
            return ValidationResult.valid();
        }

        LocalDate settlementDate;
        try {
            settlementDate = LocalDate.parse(rawDate);
        } catch (DateTimeParseException e) {
            // The schema already constrains this to xs:date, so this is a defensive path
            // rather than an expected one — but a rule that can throw is a rule that can
            // turn a rejection into a 500.
            return ValidationResult.rejected(RejectReasonCode.DT01,
                    "Settlement date is not a valid date", "GrpHdr/IntrBkSttlmDt");
        }

        LocalDate today = LocalDate.now(clock);

        if (settlementDate.isBefore(today)) {
            return ValidationResult.rejected(RejectReasonCode.DT01,
                    "Settlement date must not be in the past", "GrpHdr/IntrBkSttlmDt");
        }

        LocalDate latestAcceptable = today.plusDays(properties.maxFutureSettlementDays());
        if (settlementDate.isAfter(latestAcceptable)) {
            return ValidationResult.rejected(RejectReasonCode.DT01,
                    "Settlement date must be no more than %d days ahead"
                            .formatted(properties.maxFutureSettlementDays()),
                    "GrpHdr/IntrBkSttlmDt");
        }

        return ValidationResult.valid();
    }
}
