package com.rbc.paymentvalidation.repository;

import com.rbc.paymentvalidation.domain.Account;
import com.rbc.paymentvalidation.domain.Institution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Account maintenance and lookup.
 *
 * Accounts are identified by number and institution together, since the same number may be
 * issued by two institutions and would then mean two unrelated accounts.
 */
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Accounts are identified by number and institution: the same number may be
     * issued by two institutions and would then denote two unrelated accounts.
     */
    Optional<Account> findByAccountNumberAndInstitution(String accountNumber,
                                                        Institution institution);

    /** Supports answering "which accounts do we hold for this customer?" */
    List<Account> findByCustomerId(Long customerId);
}
