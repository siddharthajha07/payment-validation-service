package com.rbc.paymentvalidation.repository;

import com.rbc.paymentvalidation.domain.Institution;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Lookup of participating institutions, used by the validators to resolve a BIC. */
public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByBic(String bic);

    /**
     * A suspended participant is inactive rather than deleted, so historical payments
     * still reference a row that exists. Validation must therefore ask for an active one.
     */
    Optional<Institution> findByBicAndActiveTrue(String bic);
}
