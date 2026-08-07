package com.rbc.paymentvalidation.service;

import com.rbc.paymentvalidation.domain.Account;
import com.rbc.paymentvalidation.domain.AuditEventType;
import com.rbc.paymentvalidation.domain.Customer;
import com.rbc.paymentvalidation.domain.Institution;
import com.rbc.paymentvalidation.domain.Payment;
import com.rbc.paymentvalidation.domain.PaymentStatus;
import com.rbc.paymentvalidation.mapper.PaymentMapper;
import com.rbc.paymentvalidation.repository.InstitutionRepository;
import com.rbc.paymentvalidation.repository.PaymentRepository;
import com.rbc.paymentvalidation.validation.ValidationError;
import com.rbc.paymentvalidation.xml.model.envelope.Pacs008Message;
import com.rbc.paymentvalidation.xml.model.pacs008.BranchAndFinancialInstitutionIdentification;
import com.rbc.paymentvalidation.xml.model.pacs008.CreditTransferTransaction;
import com.rbc.paymentvalidation.xml.model.pacs008.PartyIdentification;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes the outcome of processing a message to the database.
 *
 * <h2>Two paths, deliberately asymmetric</h2>
 * Accepting a payment stores the payment and creates or refreshes the customers and
 * accounts it names. Rejecting one stores the payment for troubleshooting but touches no
 * customer data: a message that failed validation is not a trustworthy source of truth
 * about a customer, and learning from messages you have just declared invalid is the
 * easiest way to corrupt a customer store.
 *
 * <h2>Why the whole message is one transaction</h2>
 * A pacs.008 is a batch. Storing some of its transactions and not others would leave the
 * database describing a message that was never sent. The method is transactional so the
 * batch is recorded completely or not at all.
 */
@Service
public class PaymentRecordingService {

    private static final Logger log = LoggerFactory.getLogger(PaymentRecordingService.class);

    private final PaymentRepository paymentRepository;
    private final InstitutionRepository institutionRepository;
    private final CustomerService customerService;
    private final AuditService auditService;
    private final PaymentMapper paymentMapper;

    public PaymentRecordingService(PaymentRepository paymentRepository,
                                   InstitutionRepository institutionRepository,
                                   CustomerService customerService, AuditService auditService,
                                   PaymentMapper paymentMapper) {
        this.paymentRepository = paymentRepository;
        this.institutionRepository = institutionRepository;
        this.customerService = customerService;
        this.auditService = auditService;
        this.paymentMapper = paymentMapper;
    }

    /**
     * Records an accepted message: its payments, and the customers and accounts it names.
     *
     * @return the stored payments, in message order
     */
    @Transactional
    public List<Payment> recordAcceptance(Pacs008Message message, String correlationId) {
        List<Payment> stored = new ArrayList<>();

        for (CreditTransferTransaction transaction
                : message.getCreditTransfer().getCreditTransferTransactions()) {
            Payment payment = paymentMapper.toPayment(
                    message.getApplicationHeader(),
                    message.getCreditTransfer().getGroupHeader(),
                    transaction, PaymentStatus.ACCEPTED, correlationId);

            payment.setDebtorAccount(resolveAccount(transaction.getDebtorAgent(),
                    transaction.getDebtorAccount().accountNumber(),
                    transaction.getUltimateDebtor(), transaction.getDebtor(), correlationId));
            payment.setCreditorAccount(resolveAccount(transaction.getCreditorAgent(),
                    transaction.getCreditorAccount().accountNumber(),
                    transaction.getUltimateCreditor(), transaction.getCreditor(), correlationId));

            Payment saved = paymentRepository.save(payment);
            auditService.record(correlationId, AuditEventType.PAYMENT_RECORDED, saved.getId(),
                    "Accepted payment recorded for transaction "
                            + saved.getTransactionId());
            stored.add(saved);
        }

        log.info("Recorded {} accepted payment(s)", stored.size());
        return stored;
    }

    /**
     * Records a rejected message.
     *
     * <p>The audit trail always records the rejection. The payment table records it only
     * when the message carried enough identity to file it under — a message rejected for a
     * missing transaction identifier has no key to store it against, and one rejected as a
     * duplicate would collide with the payment already there. In both cases the audit event
     * is the record of the attempt, which is what the trail is for.
     *
     * @return the stored payment, or {@code null} when only an audit event was written
     */
    @Transactional
    public Payment recordRejection(Pacs008Message message, ValidationError error,
                                   String correlationId) {
        auditService.record(correlationId, AuditEventType.VALIDATION_FAILED,
                "Rejected with %s: %s".formatted(error.reasonCode(), error.describe()));

        CreditTransferTransaction transaction = firstStorableTransaction(message);
        if (transaction == null) {
            log.info("Rejection recorded on the audit trail only; message carried no "
                    + "storable transaction");
            return null;
        }

        String transactionId = transaction.getPaymentIdentification()
                .getTransactionIdentification();
        if (paymentRepository.existsByTransactionId(transactionId)) {
            // The payment already on file is the record; a second row would violate the
            // unique constraint and tell us nothing the first does not.
            log.info("Rejection recorded on the audit trail only; transaction already stored");
            return null;
        }

        Payment payment = paymentMapper.toPayment(message.getApplicationHeader(),
                message.getCreditTransfer().getGroupHeader(), transaction,
                PaymentStatus.REJECTED, correlationId);
        payment.recordRejection(error.reasonCode().name(), error.describe());

        Payment saved = paymentRepository.save(payment);
        auditService.record(correlationId, AuditEventType.PAYMENT_RECORDED, saved.getId(),
                "Rejected payment recorded for transaction " + saved.getTransactionId());
        return saved;
    }

    /**
     * @return the first transaction carrying the minimum data needed to store a payment,
     *         or {@code null} if none does
     */
    private CreditTransferTransaction firstStorableTransaction(Pacs008Message message) {
        if (message.getCreditTransfer() == null
                || message.getCreditTransfer().getGroupHeader() == null) {
            return null;
        }
        return message.getCreditTransfer().getCreditTransferTransactions().stream()
                .filter(this::isStorable)
                .findFirst()
                .orElse(null);
    }

    private boolean isStorable(CreditTransferTransaction transaction) {
        return transaction.getPaymentIdentification() != null
                && transaction.getPaymentIdentification().getTransactionIdentification() != null
                && transaction.getInterbankSettlementAmount() != null
                && transaction.getInterbankSettlementAmount().getValue() != null
                && transaction.getInterbankSettlementAmount().getCurrency() != null
                && transaction.getDebtorAgent() != null
                && transaction.getCreditorAgent() != null;
    }

    private Account resolveAccount(BranchAndFinancialInstitutionIdentification agent,
                                   String accountNumber, PartyIdentification ultimateParty,
                                   PartyIdentification directParty, String correlationId) {
        Institution institution = institutionRepository.findByBicAndActiveTrue(agent.bic())
                .orElseThrow(() -> new IllegalStateException(
                        "Institution %s is not active".formatted(agent.bic())));

        Customer customer = customerService.createOrUpdate(
                customerReference(ultimateParty, directParty),
                partyName(directParty, ultimateParty),
                correlationId);

        return customerService.resolveAccount(accountNumber, agent.transitNumber(),
                institution, customer, correlationId);
    }

    /**
     * @return the organisation identifier for the party, preferring the ultimate party.
     *         The ultimate debtor or creditor is the party on whose behalf the payment is
     *         ultimately made, and in the supplied samples it is the one carrying the
     *         identifier, while the direct party carries only a name.
     */
    private String customerReference(PartyIdentification ultimateParty,
                                     PartyIdentification directParty) {
        String reference = ultimateParty == null ? null : ultimateParty.customerReference();
        if (reference != null) {
            return reference;
        }
        return directParty == null ? null : directParty.customerReference();
    }

    /** @return the party name, preferring the directly named party. */
    private String partyName(PartyIdentification directParty, PartyIdentification ultimateParty) {
        String name = directParty == null ? null : directParty.getName();
        if (name != null) {
            return name;
        }
        return ultimateParty == null ? null : ultimateParty.getName();
    }
}
