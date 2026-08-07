package com.rbc.paymentvalidation.service;

import com.rbc.paymentvalidation.domain.Account;
import com.rbc.paymentvalidation.domain.AuditEventType;
import com.rbc.paymentvalidation.domain.Customer;
import com.rbc.paymentvalidation.domain.Institution;
import com.rbc.paymentvalidation.repository.AccountRepository;
import com.rbc.paymentvalidation.repository.CustomerRepository;
import com.rbc.paymentvalidation.logging.MaskingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Creates and updates customer and account records from the parties named on a payment.
 *
 * <h2>Learning customers from traffic</h2>
 * This service has no customer onboarding process; it learns about customers from the
 * payments that reference them. The first message naming a party creates the record, and
 * later messages refine it. That is the behaviour the assessment asks for, and it is how
 * a clearing participant's shadow customer store typically works.
 *
 * <h2>Only accepted payments change customer data</h2>
 * A rejected payment updates nothing. Its data failed validation, so treating it as a
 * source of truth about a customer would let a malformed or fraudulent message rewrite
 * legitimate records — the easiest way to corrupt a customer store is to learn from
 * messages you have just declared invalid.
 *
 * <h2>Concurrent first sighting</h2>
 * Two payments naming the same previously unknown customer can be processed at once. Both
 * find nothing, both insert, and the unique constraint on the customer reference rejects
 * the second. That surfaces as a data integrity violation, is reported as HTTP 409, and is
 * safe for the caller to retry with the same idempotency key — by which point the customer
 * exists and the second attempt takes the update path. Recovering in place was considered
 * and rejected: it would require the insert to run in its own transaction so that its
 * failure did not poison the caller's persistence context, and that machinery is not worth
 * adding when the idempotency mechanism already makes a retry correct.
 */
@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository,
                           AccountRepository accountRepository, AuditService auditService) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }

    /**
     * Creates the customer if this reference has not been seen, otherwise refreshes it.
     *
     * @param customerReference the organisation identifier from the message, or {@code null}
     * @param name              the party name carried by the message
     * @param correlationId     the request being processed
     * @return the stored customer, or {@code null} when the message carried no reference to
     *         identify one by. A party without an identifier is a valid message that simply
     *         yields no customer record; inventing a key from the name would create
     *         duplicates on every spelling variation.
     */
    public Customer createOrUpdate(String customerReference, String name, String correlationId) {
        if (customerReference == null || customerReference.isBlank()) {
            return null;
        }

        return customerRepository.findByCustomerReference(customerReference)
                .map(existing -> refresh(existing, name, correlationId))
                .orElseGet(() -> create(customerReference, name, correlationId));
    }

    private Customer create(String customerReference, String name, String correlationId) {
        Customer created = customerRepository.save(new Customer(customerReference, name));
        // The reference is an internal identifier rather than customer content, so it is
        // safe to record; the name is not, and is deliberately absent from the detail.
        auditService.record(correlationId, AuditEventType.CUSTOMER_CREATED,
                "Created customer record for reference " + customerReference);
        log.debug("Created customer for reference {}", customerReference);
        return created;
    }

    private Customer refresh(Customer existing, String name, String correlationId) {
        if (existing.updateName(name)) {
            // Only a real change is recorded. Logging an "update" that changed nothing
            // would make the trail longer without making it more informative.
            auditService.record(correlationId, AuditEventType.CUSTOMER_UPDATED,
                    "Updated customer record for reference " + existing.getCustomerReference());
            log.debug("Updated customer for reference {}", existing.getCustomerReference());
        }
        return existing;
    }

    /**
     * Finds the account at this institution, creating it if it has not been seen before.
     *
     * @param accountNumber the account as written on the message
     * @param transitNumber the branch identifier carried by the corresponding agent
     * @param institution   the institution holding the account
     * @param customer      the holder, or {@code null} if the message did not identify one
     * @return the stored account
     */
    public Account resolveAccount(String accountNumber, String transitNumber,
                                  Institution institution, Customer customer,
                                  String correlationId) {
        return accountRepository.findByAccountNumberAndInstitution(accountNumber, institution)
                .map(existing -> refreshAccount(existing, transitNumber, customer, correlationId))
                .orElseGet(() -> createAccount(accountNumber, transitNumber, institution,
                        customer, correlationId));
    }

    private Account createAccount(String accountNumber, String transitNumber,
                                  Institution institution, Customer customer,
                                  String correlationId) {
        Account created = accountRepository.save(
                new Account(accountNumber, transitNumber, institution, customer));
        // The account number is masked: an audit trail is read by people who do not all
        // need to see full account numbers, and the last four digits identify it well
        // enough to investigate with.
        auditService.record(correlationId, AuditEventType.ACCOUNT_CREATED,
                "Created account %s at %s".formatted(
                        MaskingUtil.maskAccountNumber(accountNumber), institution.getBic()));
        return created;
    }

    private Account refreshAccount(Account existing, String transitNumber, Customer customer,
                                   String correlationId) {
        if (existing.applyDetails(transitNumber, customer)) {
            auditService.record(correlationId, AuditEventType.ACCOUNT_UPDATED,
                    "Updated account %s at %s".formatted(
                            MaskingUtil.maskAccountNumber(existing.getAccountNumber()),
                            existing.getInstitution().getBic()));
        }
        return existing;
    }
}
