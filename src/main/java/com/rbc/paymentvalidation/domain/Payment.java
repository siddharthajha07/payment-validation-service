package com.rbc.paymentvalidation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A payment instruction that was received, and what we decided about it.
 *
 * Rejected payments are stored too, because that is what an operator needs when a sender asks
 * why their payment did not arrive.
 *
 * The unique transaction id makes duplicate detection a guarantee of the database rather than
 * a hope of the application: two concurrent requests can both pass an application check before
 * either inserts, and the index is what stops the second. The amount is BigDecimal because
 * double cannot represent 1.02 exactly.
 */
@Entity
@Table(name = "payment",
        indexes = {
                @Index(name = "idx_payment_transaction_id", columnList = "transaction_id",
                        unique = true),
                @Index(name = "idx_payment_end_to_end_id", columnList = "end_to_end_id"),
                @Index(name = "idx_payment_correlation_id", columnList = "correlation_id"),
                // Reconciliation asks "everything that settled on this date with this
                // outcome", so the two columns are indexed together rather than separately.
                @Index(name = "idx_payment_settlement_date_status",
                        columnList = "settlement_date, status")
        })
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** BizMsgIdr from the business header. */
    @Column(name = "business_message_id", length = 35)
    private String businessMessageId;

    /** GrpHdr/MsgId from the document. */
    @Column(name = "message_id", length = 35)
    private String messageId;

    @Column(name = "instruction_id", length = 35)
    private String instructionId;

    @Column(name = "end_to_end_id", length = 35)
    private String endToEndId;

    @Column(name = "transaction_id", nullable = false, length = 35)
    private String transactionId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 5)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "debtor_agent_bic", length = 11)
    private String debtorAgentBic;

    @Column(name = "creditor_agent_bic", length = 11)
    private String creditorAgentBic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debtor_account_id")
    private Account debtorAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creditor_account_id")
    private Account creditorAccount;

    /**
     * Stored as a string, not an ordinal. An ordinal would silently change meaning if the
     * enum constants were ever reordered, rewriting the history of every stored row.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    /** ISO external status reason code when rejected, for example AC01. */
    @Column(name = "reason_code", length = 4)
    private String reasonCode;

    /** Which rule rejected the message. Describes the rule, never the customer's data. */
    @Column(name = "reason_description", length = 255)
    private String reasonDescription;

    /** Links every row touched by one request, including its audit events. */
    @Column(name = "correlation_id", nullable = false, length = 36)
    private String correlationId;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected Payment() {
        // Required by JPA.
    }

    public Payment(String transactionId, BigDecimal amount, String currency,
                   PaymentStatus status, String correlationId) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.correlationId = correlationId;
        this.receivedAt = Instant.now();
    }

    public void recordRejection(String reasonCode, String reasonDescription) {
        this.status = PaymentStatus.REJECTED;
        this.reasonCode = reasonCode;
        this.reasonDescription = reasonDescription;
    }

    public Long getId() {
        return id;
    }

    public String getBusinessMessageId() {
        return businessMessageId;
    }

    public void setBusinessMessageId(String businessMessageId) {
        this.businessMessageId = businessMessageId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(String instructionId) {
        this.instructionId = instructionId;
    }

    public String getEndToEndId() {
        return endToEndId;
    }

    public void setEndToEndId(String endToEndId) {
        this.endToEndId = endToEndId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public String getDebtorAgentBic() {
        return debtorAgentBic;
    }

    public void setDebtorAgentBic(String debtorAgentBic) {
        this.debtorAgentBic = debtorAgentBic;
    }

    public String getCreditorAgentBic() {
        return creditorAgentBic;
    }

    public void setCreditorAgentBic(String creditorAgentBic) {
        this.creditorAgentBic = creditorAgentBic;
    }

    public Account getDebtorAccount() {
        return debtorAccount;
    }

    public void setDebtorAccount(Account debtorAccount) {
        this.debtorAccount = debtorAccount;
    }

    public Account getCreditorAccount() {
        return creditorAccount;
    }

    public void setCreditorAccount(Account creditorAccount) {
        this.creditorAccount = creditorAccount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getReasonDescription() {
        return reasonDescription;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
