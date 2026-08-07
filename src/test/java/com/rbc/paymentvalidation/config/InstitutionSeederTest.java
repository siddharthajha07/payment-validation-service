package com.rbc.paymentvalidation.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.domain.Institution;
import com.rbc.paymentvalidation.repository.InstitutionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class InstitutionSeederTest {

    @Autowired
    private InstitutionRepository institutionRepository;

    @Test
    @DisplayName("seeds the participating institutions with their account prefixes")
    void seedsInstitutions() {
        new InstitutionSeeder(institutionRepository).run(null);

        assertThat(institutionRepository.findByBic("BANKA000"))
                .get()
                .extracting(Institution::getAccountPrefix)
                .isEqualTo("FI");
        assertThat(institutionRepository.findByBic("BANKB000"))
                .get()
                .extracting(Institution::getAccountPrefix)
                .isEqualTo("RI");
    }

    @Test
    @DisplayName("gives the clearing system no account prefix")
    void clearingSystemHoldsNoCustomerAccounts() {
        // The clearing system is a valid counterparty on the business header but never a
        // debtor or creditor agent, so the account compatibility rule does not apply.
        new InstitutionSeeder(institutionRepository).run(null);

        assertThat(institutionRepository.findByBic("CBANK0IPS"))
                .get()
                .satisfies(institution -> {
                    assertThat(institution.getAccountPrefix()).isNull();
                    assertThat(institution.holdsCustomerAccounts()).isFalse();
                });
    }

    @Test
    @DisplayName("can run twice without duplicating reference data")
    void isIdempotent() {
        // The service must survive a restart against a persistent database without
        // duplicating rows or failing on the unique constraint.
        InstitutionSeeder seeder = new InstitutionSeeder(institutionRepository);

        seeder.run(null);
        seeder.run(null);

        assertThat(institutionRepository.findAll()).hasSize(3);
    }
}
