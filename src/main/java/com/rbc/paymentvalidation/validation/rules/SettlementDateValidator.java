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
 * Checks that the interbank settlement date falls within an acceptable window.
 *
 * <h2>Why a Clock is injected rather than calling LocalDate.now()</h2>
 * A rule about "today" is untestable if it asks the system for the time directly: the test
 * would have to construct dates relative to the real clock, and a test asserting that a
 * date is in the future stops being true once that date arrives. Injecting a
 * {@link Clock} lets a test fix the current date, so the assertions state exactly what
 * they mean and keep meaning it indefinitely.
 *
 * <p>This is the single most valuable habit in date-sensitive code, and it costs one
 * constructor argument.
 *
 * <h2>Why a past date is rejected</h2>
 * Settlement cannot be backdated: the funds movement it describes would have to have
 * happened already. A past date means the sender has stale configuration or the message
 * has been sitting in a queue, and in both cases processing it as though it were current
 * would settle it on the wrong day.
 *
 * <p>The date is optional in ISO 20022. When it is absent there is nothing to check, and
 * the rule passes rather than inventing a requirement the standard does not make.
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
