package com.rbc.paymentvalidation.repository;

import com.rbc.paymentvalidation.domain.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Customer maintenance: find an existing customer by its natural key, or create one. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerReference(String customerReference);
}
