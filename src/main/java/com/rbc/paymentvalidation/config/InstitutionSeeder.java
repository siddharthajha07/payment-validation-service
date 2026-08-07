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
 * Loads the participating institutions at startup.
 *
 * <h2>Why a seeder rather than data.sql</h2>
 * Spring Boot runs {@code data.sql} before Hibernate creates the schema unless
 * {@code defer-datasource-initialization} is set, which is a well-known trap that fails
 * with a confusing "table not found". Seeding from a runner removes the ordering question
 * entirely, and the code is testable and debuggable in a way a SQL script is not.
 *
 * <h2>Why it is idempotent</h2>
 * Each institution is inserted only if its BIC is absent. The service can therefore be
 * restarted against a persistent database without duplicating reference data or failing
 * on a unique constraint.
 *
 * <h2>Production note</h2>
 * A real deployment would source this from the scheme's participant registry rather than
 * a constant in the code — institutions join, leave and are suspended, and none of that
 * should require a release. The list is inlined here because the assessment defines a
 * fixed pair of institutions and an embedded database.
 */
@Component
public class InstitutionSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InstitutionSeeder.class);

    /**
     * Seed data as immutable values rather than entities.
     *
     * <p>This distinction matters. Holding {@code Institution} instances in a static field
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
     * <p>The clearing system carries no account prefix: it is a valid counterparty on the
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
