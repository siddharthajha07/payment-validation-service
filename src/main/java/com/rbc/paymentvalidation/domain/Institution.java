package com.rbc.paymentvalidation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A financial institution that may appear as an agent on a payment.
 *
 * <h2>Why the account prefix is a column rather than a rule in code</h2>
 * The assessment states that institution A uses FI-prefixed account numbers and
 * institution B uses RI-prefixed ones. Expressing that as {@code if (bic.equals(...))}
 * inside a validator would mean a code change, a build and a deployment every time a
 * participant joins the scheme or changes convention. Holding it as reference data means
 * the validator asks the institution what its prefix is, and onboarding a third
 * institution becomes an insert.
 *
 * <p>{@code accountPrefix} is nullable because not every institution holds customer
 * accounts. The clearing system in the supplied samples ({@code CBANK0IPS}) is a
 * legitimate message counterparty but never a debtor or creditor agent, so it has no
 * prefix and the compatibility rule does not apply to it.
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

    /** {@code FI}, {@code RI}, or null for an institution that holds no customer accounts. */
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
