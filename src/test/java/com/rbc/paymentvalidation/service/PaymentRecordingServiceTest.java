package com.rbc.paymentvalidation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbc.paymentvalidation.domain.AuditEvent;
import com.rbc.paymentvalidation.domain.AuditEventType;
import com.rbc.paymentvalidation.domain.Payment;
import com.rbc.paymentvalidation.domain.PaymentStatus;
import com.rbc.paymentvalidation.mapper.PaymentMapper;
import com.rbc.paymentvalidation.repository.AccountRepository;
import com.rbc.paymentvalidation.repository.AuditEventRepository;
import com.rbc.paymentvalidation.repository.CustomerRepository;
import com.rbc.paymentvalidation.repository.InstitutionRepository;
import com.rbc.paymentvalidation.repository.PaymentRepository;
import com.rbc.paymentvalidation.testsupport.ValidationFixtures;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class PaymentRecordingServiceTest {

    private static final String CORRELATION_ID = "corr-recording";

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    private PaymentRecordingService recordingService;

    @BeforeEach
    void setUp() {
        institutionRepository.save(ValidationFixtures.institutionA());
        institutionRepository.save(ValidationFixtures.institutionB());

        AuditService auditService = new AuditService(auditEventRepository);
        recordingService = new PaymentRecordingService(paymentRepository, institutionRepository,
                new CustomerService(customerRepository, accountRepository, auditService),
                auditService, new PaymentMapper());
    }

    @Test
    @DisplayName("stores an accepted payment")
    void storesAcceptedPayment() {
        recordingService.recordAcceptance(ValidationFixtures.validMessage(), CORRELATION_ID);

        assertThat(paymentRepository.findByTransactionId("B621200494113"))
                .get()
                .satisfies(payment -> {
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.ACCEPTED);
                    assertThat(payment.getAmount()).isEqualByComparingTo("1.02");
                });
    }

    @Test
    @DisplayName("creates the customer identified by the ultimate parties")
    void createsCustomerFromUltimateParties() {
        // The direct debtor carries only a name; the ultimate debtor carries the
        // organisation identifier, which is the only stable key the message offers.
        //
        // Note a quirk of the supplied sample: it carries the SAME identifier (6075857) on
        // both the ultimate debtor and the ultimate creditor, even though they are named
        // differently. One customer record therefore results, and the party processed
        // second supplies the name. That is correct behaviour for the data given — the
        // reference is the key, and a single key denotes a single customer — but it means
        // the sample describes a party paying itself. Recorded in ASSUMPTIONS.md.
        recordingService.recordAcceptance(ValidationFixtures.validMessage(), CORRELATION_ID);

        assertThat(customerRepository.findAll()).hasSize(1);
        assertThat(customerRepository.findByCustomerReference("6075857")).isPresent();
    }

    @Test
    @DisplayName("takes the customer name from the directly named party")
    void takesNameFromDirectParty() {
        // With the creditor's identifier removed, only the debtor side yields a customer,
        // so the name can be asserted without the sample's shared-identifier quirk.
        recordingService.recordAcceptance(
                ValidationFixtures.parse(ValidationFixtures.withoutUltimateCreditor()),
                "corr-direct-party");

        assertThat(customerRepository.findByCustomerReference("6075857"))
                .get()
                .satisfies(customer ->
                        assertThat(customer.getName()).isEqualTo("PYRAMID ENT MAN INC"));
    }

    @Test
    @DisplayName("creates both accounts at their respective institutions")
    void createsBothAccounts() {
        recordingService.recordAcceptance(ValidationFixtures.validMessage(), CORRELATION_ID);

        assertThat(accountRepository.findAll())
                .extracting(account -> account.getAccountNumber() + "@"
                        + account.getInstitution().getBic())
                .containsExactlyInAnyOrder("FI2003135@BANKA000", "RI1000331148@BANKB000");
    }

    @Test
    @DisplayName("links the stored payment to both accounts")
    void linksPaymentToAccounts() {
        Payment payment = recordingService.recordAcceptance(
                ValidationFixtures.validMessage(), CORRELATION_ID).get(0);

        assertThat(payment.getDebtorAccount().getAccountNumber()).isEqualTo("FI2003135");
        assertThat(payment.getCreditorAccount().getAccountNumber()).isEqualTo("RI1000331148");
    }

    @Test
    @DisplayName("stores a rejected payment with its reason code")
    void storesRejectedPayment() {
        // Rejections are what an operator most often looks up: a sender asking why their
        // payment did not arrive is asking about a rejected row.
        recordingService.recordRejection(ValidationFixtures.validMessage(),
                new ValidationError(RejectReasonCode.AC01, "Account prefix does not match",
                        "CdtTrfTxInf[0]/DbtrAcct"),
                CORRELATION_ID);

        assertThat(paymentRepository.findByTransactionId("B621200494113"))
                .get()
                .satisfies(payment -> {
                    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REJECTED);
                    assertThat(payment.getReasonCode()).isEqualTo("AC01");
                    assertThat(payment.getReasonDescription())
                            .contains("Account prefix does not match");
                });
    }

    @Test
    @DisplayName("does not touch customer data when rejecting")
    void rejectionCreatesNoCustomerData() {
        // A message that failed validation is not a trustworthy source of truth about a
        // customer. Learning from messages just declared invalid corrupts the store.
        recordingService.recordRejection(ValidationFixtures.validMessage(),
                new ValidationError(RejectReasonCode.AM03, "Currency not supported"),
                CORRELATION_ID);

        assertThat(customerRepository.findAll()).isEmpty();
        assertThat(accountRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("records a rejection on the audit trail even when no payment can be stored")
    void recordsAuditOnlyWhenTransactionAlreadyStored() {
        recordingService.recordAcceptance(ValidationFixtures.validMessage(), CORRELATION_ID);

        Payment second = recordingService.recordRejection(ValidationFixtures.validMessage(),
                new ValidationError(RejectReasonCode.AM05, "Already processed"),
                "corr-duplicate");

        assertThat(second).isNull();
        assertThat(paymentRepository.findAll()).hasSize(1);
        assertThat(auditEventRepository.findByCorrelationIdOrderByOccurredAtAsc("corr-duplicate"))
                .extracting(AuditEvent::getEventType)
                .containsExactly(AuditEventType.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("records a rejection when the message has no storable transaction")
    void recordsAuditOnlyWhenNothingStorable() {
        // A message rejected for a missing transaction identifier has no key to file a
        // payment under. The audit event is then the entire record of the attempt.
        Payment stored = recordingService.recordRejection(
                ValidationFixtures.messageWith("<TxId>B621200494113</TxId>", ""),
                new ValidationError(RejectReasonCode.FF01, "Transaction identifier missing"),
                CORRELATION_ID);

        assertThat(stored).isNull();
        assertThat(paymentRepository.findAll()).isEmpty();
        assertThat(auditEventRepository.findByCorrelationIdOrderByOccurredAtAsc(CORRELATION_ID))
                .isNotEmpty();
    }

    @Test
    @DisplayName("writes an audit trail describing the whole acceptance")
    void writesAuditTrailForAcceptance() {
        recordingService.recordAcceptance(ValidationFixtures.validMessage(), CORRELATION_ID);

        assertThat(auditEventRepository.findByCorrelationIdOrderByOccurredAtAsc(CORRELATION_ID))
                .extracting(AuditEvent::getEventType)
                .contains(AuditEventType.CUSTOMER_CREATED, AuditEventType.ACCOUNT_CREATED,
                        AuditEventType.PAYMENT_RECORDED);
    }
}
