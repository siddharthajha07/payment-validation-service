package com.rbc.paymentvalidation.validation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable limits applied by the business rules.
 *
 * These are policy rather than logic, so they are configuration. The supported currency
 * changes with the clearing system, the settlement window is a scheme decision, and the
 * transit length is a national convention.
 *
 * The transit length is worth exposing in particular: the brief specifies three digits but the
 * supplied sample carries 05605, which is five. The rule is implemented as specified and the
 * conflict is documented, so if the brief turns out to mean five it is a config change.
 */
@ConfigurationProperties(prefix = "payment.validation")
public record ValidationProperties(String supportedCurrency, int maxFractionDigits,
                                   int transitNumberLength, int maxFutureSettlementDays) {
}
