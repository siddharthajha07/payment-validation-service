package com.rbc.paymentvalidation.mapper;

import com.rbc.paymentvalidation.validation.ValidationError;
import com.rbc.paymentvalidation.xml.model.envelope.Pacs002Message;
import com.rbc.paymentvalidation.xml.model.envelope.Pacs008Message;
import com.rbc.paymentvalidation.xml.model.header.BusinessApplicationHeader;
import com.rbc.paymentvalidation.xml.model.header.HeaderParty;
import com.rbc.paymentvalidation.xml.model.pacs002.FIToFIPaymentStatusReport;
import com.rbc.paymentvalidation.xml.model.pacs002.OriginalGroupHeaderAndStatus;
import com.rbc.paymentvalidation.xml.model.pacs002.StatusGroupHeader;
import com.rbc.paymentvalidation.xml.model.pacs002.StatusReasonInformation;
import com.rbc.paymentvalidation.xml.model.pacs002.TransactionInfoAndStatus;
import com.rbc.paymentvalidation.xml.model.pacs008.CreditTransferTransaction;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Builds the pacs.002 status report returned for an incoming pacs.008.
 *
 * <h2>The header is answered, not copied</h2>
 * The response reverses the parties: its {@code Fr} is the request's {@code To} and its
 * {@code To} is the request's {@code Fr}. This service is replying as the institution the
 * payment was addressed to, and getting this backwards would produce a message that
 * appears to come from the sender itself.
 *
 * <h2>Accepted and rejected reports differ in shape</h2>
 * A rejection carries a group status of {@code RJCT} with the reason, and no transaction
 * entries: the message was refused as a whole, so there is no per-transaction outcome to
 * report. An acceptance carries {@code ACCP} together with one entry per transaction,
 * echoing all three original identifiers so that every party in the chain can match the
 * report to its own record. Both shapes follow the supplied samples.
 */
@Component
public class Pacs002Factory {

    private static final String PACS_002_MESSAGE_DEFINITION = "pacs.002.001.14";
    private static final String STATUS_ACCEPTED = "ACCP";
    private static final String STATUS_REJECTED = "RJCT";

    /** ISO convention for a value that is required but genuinely unknown. */
    private static final String NOT_PROVIDED = "NOTPROVIDED";

    private static final DateTimeFormatter MESSAGE_ID_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public Pacs002Factory(Clock clock) {
        this.clock = clock;
    }

    /** Builds an {@code ACCP} report for a message that passed every rule. */
    public Pacs002Message accept(Pacs008Message request) {
        String statusMessageId = generateMessageIdentifier();
        String timestamp = timestamp();

        List<TransactionInfoAndStatus> transactionStatuses =
                request.getCreditTransfer().getCreditTransferTransactions().stream()
                        .map(transaction -> toTransactionStatus(transaction, statusMessageId,
                                timestamp))
                        .toList();

        return new Pacs002Message(
                responseHeader(request, statusMessageId, timestamp),
                new FIToFIPaymentStatusReport(
                        groupHeader(request, statusMessageId, timestamp),
                        originalGroupStatus(request, STATUS_ACCEPTED, null),
                        transactionStatuses));
    }

    /** Builds an {@code RJCT} report carrying the ISO reason code for the failure. */
    public Pacs002Message reject(Pacs008Message request, ValidationError error) {
        String statusMessageId = generateMessageIdentifier();
        String timestamp = timestamp();

        StatusReasonInformation reasonInformation = new StatusReasonInformation(
                originatorBic(request),
                error.reasonCode().name(),
                // Describes the rule and where it failed, never the values that failed it:
                // this document travels to another institution and is stored at both ends.
                error.describe());

        return new Pacs002Message(
                responseHeader(request, statusMessageId, timestamp),
                new FIToFIPaymentStatusReport(
                        groupHeader(request, statusMessageId, timestamp),
                        originalGroupStatus(request, STATUS_REJECTED, reasonInformation),
                        List.of()));
    }

    private BusinessApplicationHeader responseHeader(Pacs008Message request,
                                                     String statusMessageId, String timestamp) {
        BusinessApplicationHeader header = new BusinessApplicationHeader();
        header.setFrom(new HeaderParty(originatorBic(request)));
        header.setTo(new HeaderParty(senderBic(request)));
        header.setBusinessMessageIdentifier(statusMessageId);
        header.setMessageDefinitionIdentifier(PACS_002_MESSAGE_DEFINITION);
        header.setBusinessService(businessService(request));
        header.setCreationDate(timestamp);
        return header;
    }

    private StatusGroupHeader groupHeader(Pacs008Message request, String statusMessageId,
                                          String timestamp) {
        // The instructed agent is the party being told the outcome: the original sender.
        return new StatusGroupHeader(statusMessageId, timestamp, senderBic(request));
    }

    private OriginalGroupHeaderAndStatus originalGroupStatus(Pacs008Message request,
                                                             String status,
                                                             StatusReasonInformation reason) {
        return new OriginalGroupHeaderAndStatus(originalMessageId(request),
                originalMessageDefinition(request), status, reason);
    }

    private TransactionInfoAndStatus toTransactionStatus(CreditTransferTransaction transaction,
                                                         String statusMessageId,
                                                         String timestamp) {
        return new TransactionInfoAndStatus(
                statusMessageId,
                transaction.getPaymentIdentification().getInstructionIdentification(),
                transaction.getPaymentIdentification().getEndToEndIdentification(),
                transaction.getPaymentIdentification().getTransactionIdentification(),
                STATUS_ACCEPTED,
                timestamp);
    }

    /**
     * @return a unique identifier for this status report, in the shape used by the supplied
     *         samples: a {@code PS} prefix, the date, and a random suffix. Random rather
     *         than sequential because a sequence would need coordinated state across
     *         instances and would leak the service's message volume to anyone receiving two
     *         of them.
     */
    private String generateMessageIdentifier() {
        return "PS%s%012d".formatted(MESSAGE_ID_DATE.format(Instant.now(clock)),
                Math.floorMod(random.nextLong(), 1_000_000_000_000L));
    }

    private String timestamp() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now(clock).truncatedTo(
                java.time.temporal.ChronoUnit.SECONDS));
    }

    /** @return the BIC this service answers as: the party the request was addressed to. */
    private String originatorBic(Pacs008Message request) {
        String bic = request.getApplicationHeader() == null
                ? null : request.getApplicationHeader().receiverBic();
        return bic == null ? NOT_PROVIDED : bic;
    }

    /** @return the BIC that sent the request, and which the response is addressed to. */
    private String senderBic(Pacs008Message request) {
        String bic = request.getApplicationHeader() == null
                ? null : request.getApplicationHeader().senderBic();
        return bic == null ? NOT_PROVIDED : bic;
    }

    private String businessService(Pacs008Message request) {
        return request.getApplicationHeader() == null
                ? null : request.getApplicationHeader().getBusinessService();
    }

    private String originalMessageId(Pacs008Message request) {
        if (request.getCreditTransfer() == null
                || request.getCreditTransfer().getGroupHeader() == null
                || request.getCreditTransfer().getGroupHeader().getMessageIdentification() == null) {
            return NOT_PROVIDED;
        }
        return request.getCreditTransfer().getGroupHeader().getMessageIdentification();
    }

    private String originalMessageDefinition(Pacs008Message request) {
        String definition = request.getApplicationHeader() == null
                ? null : request.getApplicationHeader().getMessageDefinitionIdentifier();
        return definition == null ? NOT_PROVIDED : definition;
    }
}
