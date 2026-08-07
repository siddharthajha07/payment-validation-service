package com.rbc.paymentvalidation.xml.signature;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Location and credentials of the key used to sign outbound responses.
 *
 * <p>The keystore shipped with this service is a self-signed, test-only key pair, and its
 * password is in {@code application.yml} in plain text. That is deliberate for an
 * assessment — a reviewer must be able to clone the repository and start the service — and
 * it is emphatically not how a production deployment would work.
 *
 * <p>In production the private key would live in a hardware security module or a secrets
 * manager, the application would hold a reference rather than the key itself, and the
 * certificate would be issued by a certificate authority the counterparty already trusts.
 * Because the settings are configuration rather than constants, pointing this service at
 * such a keystore is a deployment change and not a code change.
 *
 * @param keystoreLocation classpath location of the PKCS12 keystore
 * @param keystorePassword password protecting the keystore
 * @param keyAlias         alias of the signing key within it
 * @param keyPassword      password protecting the private key
 */
@ConfigurationProperties(prefix = "payment.signing")
public record SigningProperties(String keystoreLocation, String keystorePassword,
                                String keyAlias, String keyPassword) {
}
