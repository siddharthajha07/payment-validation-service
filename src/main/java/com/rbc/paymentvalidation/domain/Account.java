package com.rbc.paymentvalidation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * An account at an institution, learned from the payments that reference it.
 *
 * The unique key spans number and institution, not the number alone: two institutions can
 * issue the same number and they are different accounts.
 *
 * The customer link is optional because a payment always names an account but does not always
 * carry an identifier for the party holding it. Requiring one would mean rejecting valid
 * messages or inventing customer records with no key.
 *
 * Associations are lazy. With open-in-view off, that forces callers to load what they need
 * inside a transaction rather than triggering queries while the response is being written.
 */
@Entity
@Table(name = "account",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_account_number_institution",
                columnNames = {"account_number", "institution_id"}))
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 34)
    private String accountNumber;

    /** Branch identifier carried as BrnchId/Id on the agent. */
    @Column(name = "transit_number", length = 35)
    private String transitNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    protected Account() {
        // Required by JPA.
    }

    public Account(String accountNumber, String transitNumber, Institution institution,
                   Customer customer) {
        this.accountNumber = accountNumber;
        this.transitNumber = transitNumber;
        this.institution = institution;
        this.customer = customer;
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastUpdatedAt = now;
    }

    /**
     * Applies details carried by a later message.
     *
     * @return true if anything actually changed, so the caller can avoid recording an
     *         audit event for an update that updated nothing.
     */
    public boolean applyDetails(String newTransitNumber, Customer newCustomer) {
        boolean changed = false;
        if (newTransitNumber != null && !newTransitNumber.equals(this.transitNumber)) {
            this.transitNumber = newTransitNumber;
            changed = true;
        }
        if (newCustomer != null && !newCustomer.equals(this.customer)) {
            this.customer = newCustomer;
            changed = true;
        }
        if (changed) {
            this.lastUpdatedAt = Instant.now();
        }
        return changed;
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getTransitNumber() {
        return transitNumber;
    }

    public Institution getInstitution() {
        return institution;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
