package com.rbc.paymentvalidation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A financial institution that can appear as an agent on a payment.
 *
 * The account prefix is a column rather than a rule in code. The brief says institution A uses
 * FI-prefixed accounts and B uses RI; writing that as an if statement in a validator would
 * mean a code change, build and deployment every time a participant joins or changes
 * convention. As reference data, onboarding a third institution is an insert.
 *
 * The prefix is nullable because not every institution holds customer accounts. The clearing
 * system is a legitimate counterparty on the business header but never a debtor or creditor
 * agent.
 *
 * Suspension is a flag rather than a delete, so historical payments keep pointing at a row
 * that still exists.
 */
@Entity
@Table(name = "institution")
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bic", nullable = false, unique = true, length = 11)
    private String bic;

    @Column(name = "name", length = 140)
    private String name;

    /** FI, RI, or null for an institution that holds no customer accounts. */
    @Column(name = "account_prefix", length = 4)
    private String accountPrefix;

    /**
     * Whether the institution may currently participate. Suspending a participant is a
     * flag rather than a delete, so historical payments continue to reference a row that
     * still exists.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Institution() {
        // Required by JPA.
    }

    public Institution(String bic, String name, String accountPrefix, boolean active) {
        this.bic = bic;
        this.name = name;
        this.accountPrefix = accountPrefix;
        this.active = active;
        this.createdAt = Instant.now();
    }

    /** @return true if this institution holds customer accounts under a naming convention. */
    public boolean holdsCustomerAccounts() {
        return accountPrefix != null && !accountPrefix.isBlank();
    }

    public Long getId() {
        return id;
    }

    public String getBic() {
        return bic;
    }

    public String getName() {
        return name;
    }

    public String getAccountPrefix() {
        return accountPrefix;
    }

    public void setAccountPrefix(String accountPrefix) {
        this.accountPrefix = accountPrefix;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
