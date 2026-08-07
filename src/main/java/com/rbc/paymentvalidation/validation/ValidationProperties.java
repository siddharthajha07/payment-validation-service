package com.rbc.paymentvalidation.validation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable limits applied by the business rules.
 *
 * <p>These are configuration rather than constants because they are policy, not logic. The
 * supported currency changes when the service is deployed against a different clearing
 * system; the settlement window is a scheme decision; the transit length is a national
 * convention. Recompiling to change any of them would be the wrong shape of answer.
 *
 * <p>The transit number length is particularly worth exposing. The assessment specifies
 * three digits, but the supplied sample carries {@code 05605}, which is five. The rule is
 * implemented as specified and the conflict is documented; holding the length here means
 * that if the specification turns out to mean five, it is a configuration change and not a
 * code change.
 *
 * @param supportedCurrency        the only currency this deployment settles in
 * @param maxFractionDigits        decimal places permitted on an amount
 * @param transitNumberLength      required number of digits in a branch transit identifier
 * @param maxFutureSettlementDays  how far ahead a settlement date may be dated
 */
@ConfigurationProperties(prefix = "payment.validation")
public record ValidationProperties(String supportedCurrency, int maxFractionDigits,
                                   int transitNumberLength, int maxFutureSettlementDays) {
}
