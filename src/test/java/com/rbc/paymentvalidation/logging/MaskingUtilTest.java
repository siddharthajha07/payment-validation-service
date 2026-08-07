package com.rbc.paymentvalidation.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the masking rules.
 *
 * <p>These exist to make a stated intention enforceable. "We do not log account numbers" is
 * a claim until something fails when the claim stops being true.
 */
class MaskingUtilTest {

    @Test
    @DisplayName("shows only the last four characters of an account number")
    void showsOnlyTheLastFourCharacters() {
        assertThat(MaskingUtil.maskAccountNumber("FI2003135")).isEqualTo("****3135");
        assertThat(MaskingUtil.maskAccountNumber("RI1000331148")).isEqualTo("****1148");
    }

    @Test
    @DisplayName("never leaks the leading digits of an account number")
    void neverLeaksLeadingDigits() {
        assertThat(MaskingUtil.maskAccountNumber("RI1000331148")).doesNotContain("1000331");
    }

    @Test
    @DisplayName("masks a short account number entirely")
    void masksShortAccountNumberEntirely() {
        // "Last four" of a four-character value would reveal all of it. A rule that
        // silently stops protecting small inputs is one nobody can rely on.
        assertThat(MaskingUtil.maskAccountNumber("1234")).isEqualTo("****");
        assertThat(MaskingUtil.maskAccountNumber("12")).isEqualTo("****");
    }

    @Test
    @DisplayName("masks an absent account number rather than printing null")
    void masksAbsentAccountNumber() {
        assertThat(MaskingUtil.maskAccountNumber(null)).isEqualTo("****");
        assertThat(MaskingUtil.maskAccountNumber("   ")).isEqualTo("****");
    }

    @Test
    @DisplayName("records that a value was present without disclosing it")
    void redactsWithoutDisclosing() {
        assertThat(MaskingUtil.redact("PYRAMID ENT MAN INC")).isEqualTo("[redacted]");
        assertThat(MaskingUtil.redact(null)).isEqualTo("[absent]");
    }

    @Test
    @DisplayName("describes a payload by its size and nothing else")
    void describesPayloadBySizeOnly() {
        // Every log statement tempted to include the message body uses this instead. The
        // size is enough to diagnose a truncation or an oversized request.
        byte[] payload = "<Message><Dbtr><Nm>ACME</Nm></Dbtr></Message>"
                .getBytes(StandardCharsets.UTF_8);

        assertThat(MaskingUtil.describePayload(payload))
                .isEqualTo("[%d bytes]".formatted(payload.length))
                .doesNotContain("ACME");
    }
}
