package com.rbc.paymentvalidation.config;

import com.rbc.paymentvalidation.domain.Institution;
import com.rbc.paymentvalidation.repository.InstitutionRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the participating institutions at startup, inserting only what is missing.
 *
 * A runner rather than data.sql, which Spring Boot runs before Hibernate creates the schema
 * unless defer-datasource-initialization is set. A real deployment would read the scheme's
 * participant registry instead, since institutions join and leave without a release.
 */
@Component
public class InstitutionSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InstitutionSeeder.class);

    /**
     * Seed data as immutable values rather than entities.
     *
     * This distinction matters. Holding Institution instances in a static field
     * would share mutable persistent state across the whole JVM: on the first save
     * Hibernate writes the generated identifier back into the shared object, so the next
     * save sees an entity that already has an identifier and issues an update against a
     * row that may not exist. Constructing a fresh entity per insert keeps persistence
     * state where it belongs — inside a persistence context.
     */
    private record InstitutionSeed(String bic, String name, String accountPrefix) {
    }

    /**
     * The institutions this service recognises.
     *
     * The clearing system carries no account prefix: it is a valid counterparty on the
     * business header but never a debtor or creditor agent, so the account compatibility
     * rule does not apply to it.
     */
    private static final List<InstitutionSeed> SEED = List.of(
            new InstitutionSeed("BANKA000", "Institution A", "FI"),
            new InstitutionSeed("BANKB000", "Institution B", "RI"),
            new InstitutionSeed("CBANK0IPS", "Central Bank Instant Payment System", null));

    private final InstitutionRepository institutionRepository;

    public InstitutionSeeder(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (InstitutionSeed seed : SEED) {
            if (institutionRepository.findByBic(seed.bic()).isEmpty()) {
                institutionRepository.save(
                        new Institution(seed.bic(), seed.name(), seed.accountPrefix(), true));
                log.info("Seeded institution {} with account prefix {}",
                        seed.bic(),
                        seed.accountPrefix() == null ? "none" : seed.accountPrefix());
            }
        }
    }
}
