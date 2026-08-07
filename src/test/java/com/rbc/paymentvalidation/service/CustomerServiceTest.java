package com.rbc.paymentvalidation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.domain.Account;
import com.rbc.paymentvalidation.domain.AuditEvent;
import com.rbc.paymentvalidation.domain.AuditEventType;
import com.rbc.paymentvalidation.domain.Customer;
import com.rbc.paymentvalidation.domain.Institution;
import com.rbc.paymentvalidation.repository.AccountRepository;
import com.rbc.paymentvalidation.repository.AuditEventRepository;
import com.rbc.paymentvalidation.repository.CustomerRepository;
import com.rbc.paymentvalidation.repository.InstitutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class CustomerServiceTest {

    private static final String CORRELATION_ID = "corr-customer";

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private CustomerService customerService;
    private Institution institutionA;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, accountRepository,
                new AuditService(auditEventRepository));
        institutionA = institutionRepository.save(
                new Institution("BANKA000", "Institution A", "FI", true));
    }

    @Test
    @DisplayName("creates a customer the first time a reference is seen")
    void createsCustomerOnFirstSighting() {
        Customer customer = customerService.createOrUpdate("6075857", "PYRAMID ENT MAN INC",
                CORRELATION_ID);

        assertThat(customer.getCustomerReference()).isEqualTo("6075857");
        assertThat(customerRepository.findByCustomerReference("6075857")).isPresent();
    }

    @Test
    @DisplayName("reuses the existing customer when the reference is seen again")
    void reusesCustomerOnSecondSighting() {
        Customer first = customerService.createOrUpdate("6075857", "PYRAMID", CORRELATION_ID);
        Customer second = customerService.createOrUpdate("6075857", "PYRAMID", CORRELATION_ID);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(customerRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("applies a changed name from a later message")
    void appliesChangedName() {
        customerService.createOrUpdate("6075857", "PYRAMID ENT MAN INC", CORRELATION_ID);
        Customer updated = customerService.createOrUpdate("6075857",
                "PYRAMID ENTERPRISE MANAGEMENT INC", CORRELATION_ID);

        assertThat(updated.getName()).isEqualTo("PYRAMID ENTERPRISE MANAGEMENT INC");
    }

    @Test
    @DisplayName("returns no customer when the message carried no identifier")
    void returnsNoCustomerWithoutReference() {
        // A party with a name but no organisation identifier is a valid message. Keying a
        // customer off the name instead would create a duplicate on every spelling change.
        assertThat(customerService.createOrUpdate(null, "PYRAMID", CORRELATION_ID)).isNull();
        assertThat(customerService.createOrUpdate("  ", "PYRAMID", CORRELATION_ID)).isNull();
        assertThat(customerRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("records creation and update on the audit trail, but not a no-op update")
    void recordsOnlyRealChanges() {
        customerService.createOrUpdate("6075857", "PYRAMID", CORRELATION_ID);
        customerService.createOrUpdate("6075857", "PYRAMID", CORRELATION_ID);
        customerService.createOrUpdate("6075857", "PYRAMID LTD", CORRELATION_ID);

        assertThat(auditEventRepository.findByCorrelationIdOrderByOccurredAtAsc(CORRELATION_ID))
                .extracting(AuditEvent::getEventType)
                .containsExactly(AuditEventType.CUSTOMER_CREATED, AuditEventType.CUSTOMER_UPDATED);
    }

    @Test
    @DisplayName("never writes a customer name to the audit trail")
    void neverWritesCustomerNameToAuditTrail() {
        customerService.createOrUpdate("6075857", "PYRAMID ENT MAN INC", CORRELATION_ID);

        assertThat(auditEventRepository.findByCorrelationIdOrderByOccurredAtAsc(CORRELATION_ID))
                .extracting(AuditEvent::getDetail)
                .noneMatch(detail -> detail.contains("PYRAMID"));
    }

    @Test
    @DisplayName("creates an account the first time it is seen at an institution")
    void createsAccountOnFirstSighting() {
        Account account = customerService.resolveAccount("FI2003135", "056", institutionA, null,
                CORRELATION_ID);

        assertThat(account.getAccountNumber()).isEqualTo("FI2003135");
        assertThat(account.getInstitution().getBic()).isEqualTo("BANKA000");
    }

    @Test
    @DisplayName("reuses an existing account rather than creating a second")
    void reusesExistingAccount() {
        Account first = customerService.resolveAccount("FI2003135", "056", institutionA, null,
                CORRELATION_ID);
        Account second = customerService.resolveAccount("FI2003135", "056", institutionA, null,
                CORRELATION_ID);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(accountRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("attaches the customer to an account first seen without one")
    void attachesCustomerToExistingAccount() {
        customerService.resolveAccount("FI2003135", "056", institutionA, null, CORRELATION_ID);
        Customer customer = customerService.createOrUpdate("6075857", "PYRAMID", CORRELATION_ID);

        Account updated = customerService.resolveAccount("FI2003135", "056", institutionA,
                customer, CORRELATION_ID);

        assertThat(updated.getCustomer()).isNotNull();
        assertThat(updated.getCustomer().getCustomerReference()).isEqualTo("6075857");
    }

    @Test
    @DisplayName("masks the account number written to the audit trail")
    void masksAccountNumberInAuditTrail() {
        // The trail is read by people who do not all need to see full account numbers.
        // The last four digits identify the account well enough to investigate with.
        customerService.resolveAccount("FI2003135", "056", institutionA, null, CORRELATION_ID);

        assertThat(auditEventRepository.findByCorrelationIdOrderByOccurredAtAsc(CORRELATION_ID))
                .extracting(AuditEvent::getDetail)
                .allSatisfy(detail -> {
                    assertThat(detail).doesNotContain("FI2003135");
                    assertThat(detail).contains("****3135");
                });
    }
}
