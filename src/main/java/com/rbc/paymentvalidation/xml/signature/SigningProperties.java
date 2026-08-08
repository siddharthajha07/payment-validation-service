package com.rbc.paymentvalidation.xml.signature;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where the signing key lives and how to open it.
 *
 * The keystore shipped here is a self-signed test key with its password in plain text, so the
 * service runs straight from a clone. Production would keep the private key in an HSM or a
 * secrets manager and use a certificate from a CA the counterparty trusts. Because these are
 * configuration values, pointing at such a keystore is a deployment change, not a code change.
 */
@ConfigurationProperties(prefix = "payment.signing")
public record SigningProperties(String keystoreLocation, String keystorePassword,
                                String keyAlias, String keyPassword) {
}
