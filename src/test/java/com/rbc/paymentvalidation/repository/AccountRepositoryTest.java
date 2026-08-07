package com.rbc.paymentvalidation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rbc.paymentvalidation.domain.Account;
import com.rbc.paymentvalidation.domain.Customer;
import com.rbc.paymentvalidation.domain.Institution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Institution institutionA;
    private Institution institutionB;

    @BeforeEach
    void createInstitutions() {
        institutionA = institutionRepository.save(
                new Institution("BANKA000", "Institution A", "FI", true));
        institutionB = institutionRepository.save(
                new Institution("BANKB000", "Institution B", "RI", true));
    }

    @Test
    @DisplayName("finds an account by number and institution together")
    void findsAccountByNumberAndInstitution() {
        accountRepository.save(new Account("FI2003135", "056", institutionA, null));

        assertThat(accountRepository.findByAccountNumberAndInstitution("FI2003135", institutionA))
                .isPresent();
    }

    @Test
    @DisplayName("treats the same number at a different institution as a different account")
    void sameNumberAtDifferentInstitutionIsADifferentAccount() {
        // Two institutions may legitimately issue the same account number. A unique
        // constraint on the number alone would refuse the second and conflate two
        // unrelated customers.
        accountRepository.saveAndFlush(new Account("1000331148", "056", institutionA, null));
        accountRepository.saveAndFlush(new Account("1000331148", "096", institutionB, null));

        assertThat(accountRepository.findAll()).hasSize(2);
        assertThat(accountRepository.findByAccountNumberAndInstitution("1000331148", institutionB))
                .isPresent()
                .get()
                .extracting(account -> account.getInstitution().getBic())
                .isEqualTo("BANKB000");
    }

    @Test
    @DisplayName("refuses the same number twice at one institution")
    void refusesDuplicateNumberAtSameInstitution() {
        accountRepository.saveAndFlush(new Account("FI2003135", "056", institutionA, null));

        assertThatThrownBy(() ->
                accountRepository.saveAndFlush(new Account("FI2003135", "057", institutionA, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("allows an account whose holder is not yet identified")
    void allowsAccountWithoutCustomer() {
        // A message always names an account but does not always carry an organisation
        // identifier for the party holding it.
        Account saved = accountRepository.save(new Account("FI2003135", "056", institutionA, null));

        assertThat(saved.getCustomer()).isNull();
    }

    @Test
    @DisplayName("lists the accounts held by one customer")
    void listsAccountsForCustomer() {
        Customer customer = customerRepository.save(new Customer("6075857", "PYRAMID"));
        accountRepository.save(new Account("FI2003135", "056", institutionA, customer));
        accountRepository.save(new Account("FI2003136", "056", institutionA, customer));

        assertThat(accountRepository.findByCustomerId(customer.getId())).hasSize(2);
    }

    @Test
    @DisplayName("reports whether applying details actually changed anything")
    void reportsWhetherDetailsChanged() {
        Account account = new Account("FI2003135", "056", institutionA, null);

        assertThat(account.applyDetails("056", null)).isFalse();
        assertThat(account.applyDetails("057", null)).isTrue();
        assertThat(account.getTransitNumber()).isEqualTo("057");
    }
}
