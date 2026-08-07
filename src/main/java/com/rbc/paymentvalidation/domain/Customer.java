package com.rbc.paymentvalidation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * A customer, created or updated from the parties named on an incoming payment.
 *
 * <h2>The natural key</h2>
 * {@code customerReference} is the organisation identifier carried at
 * {@code Id/OrgId/Othr/Id} on the ultimate debtor or creditor. It is the only stable
 * identifier the message offers: names are free text and change spelling between
 * messages, so matching on name would create duplicates and merge unrelated parties.
 * A unique constraint on the reference is what makes "create or update" safe.
 *
 * <h2>Why there is a version column</h2>
 * Two payments naming the same customer can be processed concurrently. Without optimistic
 * locking, both would read the same row, both would apply their changes, and the second
 * write would silently discard the first. {@code @Version} makes the second write fail
 * loudly instead, so it can be retried against the current state.
 */
@Entity
@Table(name = "customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_reference", nullable = false, unique = true, length = 35)
    private String customerReference;

    @Column(name = "name", length = 140)
    private String name;

    /** When this customer was first seen. Never changes once written. */
    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Customer() {
        // Required by JPA.
    }

    public Customer(String customerReference, String name) {
        this.customerReference = customerReference;
        this.name = name;
        Instant now = Instant.now();
        this.firstSeenAt = now;
        this.lastUpdatedAt = now;
    }

    /**
     * Applies the name carried by a later message.
     *
     * @return true if anything actually changed, so that the caller can decide whether an
     *         audit event is warranted — recording an "update" that changed nothing would
     *         make the audit trail noisier without making it more informative.
     */
    public boolean updateName(String newName) {
        if (newName == null || newName.equals(this.name)) {
            return false;
        }
        this.name = newName;
        this.lastUpdatedAt = Instant.now();
        return true;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerReference() {
        return customerReference;
    }

    public String getName() {
        return name;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
