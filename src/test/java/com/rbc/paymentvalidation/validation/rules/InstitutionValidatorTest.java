package com.rbc.paymentvalidation.validation.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rbc.paymentvalidation.domain.Institution;
import com.rbc.paymentvalidation.repository.InstitutionRepository;
import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationResult;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InstitutionValidatorTest {

    private final InstitutionRepository institutionRepository = mock(InstitutionRepository.class);
    private final InstitutionValidator validator = new InstitutionValidator(institutionRepository);

    @BeforeEach
    void stubKnownInstitutions() {
        when(institutionRepository.findByBicAndActiveTrue("BANKA000"))
                .thenReturn(Optional.of(ValidationFixtures.institutionA()));
        when(institutionRepository.findByBicAndActiveTrue("BANKB000"))
                .thenReturn(Optional.of(ValidationFixtures.institutionB()));
        when(institutionRepository.findByBicAndActiveTrue("CBANK0IPS"))
                .thenReturn(Optional.of(ValidationFixtures.clearingSystem()));
    }

    @Test
    @DisplayName("accepts a message naming only known active institutions")
    void acceptsKnownInstitutions() {
        assertThat(validator.validate(ValidationFixtures.validContext()).isValid()).isTrue();
    }

    @Test
    @DisplayName("rejects a message naming an institution this service does not recognise")
    void rejectsUnknownInstitution() {
        ValidationResult result = validator.validate(ValidationFixtures.contextOf(
                ValidationFixtures.messageWith("<BICFI>BANKB000</BICFI>",
                        "<BICFI>BANKZ999</BICFI>")));

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.RC01);
        assertThat(result.primaryError().message())
                .isEqualTo("Institution BANKZ999 is not a recognised active participant");
    }

    @Test
    @DisplayName("rejects a known institution that has been suspended")
    void rejectsSuspendedInstitution() {
        // Recognising the BIC is not enough. A suspended participant is marked inactive
        // rather than deleted so historical payments still resolve, which means the lookup
        // must ask for an active one — this test is what stops that being forgotten.
        when(institutionRepository.findByBicAndActiveTrue("BANKB000")).thenReturn(Optional.empty());

        ValidationResult result = validator.validate(ValidationFixtures.validContext());

        assertThat(result.primaryError().reasonCode()).isEqualTo(RejectReasonCode.RC01);
    }

    @Test
    @DisplayName("checks the agents inside the document, not only the header parties")
    void checksAgentsAsWellAsHeaderParties() {
        Institution suspended = new Institution("BANKA000", "Institution A", "FI", false);
        when(institutionRepository.findByBicAndActiveTrue("BANKA000")).thenReturn(Optional.empty());

        ValidationResult result = validator.validate(ValidationFixtures.validContext());

        // BANKA000 appears as the header sender, the instructing agent and the debtor
        // agent, so an unknown BIC there is reported for each place it is named.
        assertThat(result.errors()).hasSizeGreaterThan(1);
        assertThat(suspended.isActive()).isFalse();
    }
}
