package com.rbc.paymentvalidation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rbc.paymentvalidation.domain.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Tests for customer maintenance.
 *
 * @DataJpaTest starts only the persistence layer against an in-memory database
 * and rolls each test back afterwards, so these run in milliseconds and cannot leak state
 * into one another.
 */
@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    @DisplayName("finds a customer by its natural key")
    void findsCustomerByReference() {
        customerRepository.save(new Customer("6075857", "PYRAMID ENT MAN INC"));

        assertThat(customerRepository.findByCustomerReference("6075857"))
                .isPresent()
                .get()
                .extracting(Customer::getName)
                .isEqualTo("PYRAMID ENT MAN INC");
    }

    @Test
    @DisplayName("refuses a second customer with the same reference")
    void refusesDuplicateReference() {
        // This constraint is what makes "create or update" safe: without it, concurrent
        // messages naming the same customer would each create their own row.
        customerRepository.saveAndFlush(new Customer("6075857", "PYRAMID ENT MAN INC"));

        assertThatThrownBy(() ->
                customerRepository.saveAndFlush(new Customer("6075857", "SOMEONE ELSE")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("reports whether an update actually changed anything")
    void reportsWhetherUpdateChangedAnything() {
        // The caller uses this to decide whether an audit event is warranted; recording an
        // update that changed nothing would add noise without adding information.
        Customer customer = new Customer("6075857", "PYRAMID ENT MAN INC");

        assertThat(customer.updateName("PYRAMID ENT MAN INC")).isFalse();
        assertThat(customer.updateName("PYRAMID ENTERPRISE MANAGEMENT INC")).isTrue();
        assertThat(customer.getName()).isEqualTo("PYRAMID ENTERPRISE MANAGEMENT INC");
    }

    @Test
    @DisplayName("ignores a null name rather than erasing the one already held")
    void ignoresNullName() {
        Customer customer = new Customer("6075857", "PYRAMID ENT MAN INC");

        assertThat(customer.updateName(null)).isFalse();
        assertThat(customer.getName()).isEqualTo("PYRAMID ENT MAN INC");
    }

    @Test
    @DisplayName("carries a version so concurrent updates cannot silently overwrite")
    void carriesVersionForOptimisticLocking() {
        Customer saved = customerRepository.saveAndFlush(
                new Customer("6075857", "PYRAMID ENT MAN INC"));

        assertThat(saved.getVersion()).isNotNull();
    }

    @Test
    @DisplayName("records when the customer was first seen and last changed")
    void recordsTimestamps() {
        Customer saved = customerRepository.save(new Customer("6075857", "PYRAMID"));

        assertThat(saved.getFirstSeenAt()).isNotNull();
        assertThat(saved.getLastUpdatedAt()).isNotNull();
    }
}
