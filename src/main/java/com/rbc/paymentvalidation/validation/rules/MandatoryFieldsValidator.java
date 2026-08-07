package com.rbc.paymentvalidation.validation.rules;

import com.rbc.paymentvalidation.validation.PaymentValidator;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationContext;
import com.rbc.paymentvalidation.validation.ValidationError;
import com.rbc.paymentvalidation.validation.ValidationResult;
import com.rbc.paymentvalidation.xml.model.pacs008.CreditTransferTransaction;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Checks that every element this service needs is actually present.
 *
 * The schema enforces what ISO requires; this enforces what we require, and they differ. TxId
 * is the clearest case: ISO makes it optional, but duplicate detection depends on it, so a
 * message without one cannot be processed safely.
 *
 * It runs first so no later rule has to guard against a missing element. Unlike the others it
 * reports everything it finds rather than stopping at the first, because someone repairing a
 * message should learn all of it in one exchange.
 */
@Component
@Order(10)
public class MandatoryFieldsValidator implements PaymentValidator {

    private static final String EXPECTED_MESSAGE_DEFINITION = "pacs.008.001.12";

    @Override
    public ValidationResult validate(ValidationContext context) {
        List<ValidationError> errors = new ArrayList<>();

        if (context.header() == null) {
            return ValidationResult.rejected(RejectReasonCode.FF01,
                    "Business application header is missing", "AppHdr");
        }
        requirePresent(context.senderBic(), "AppHdr/Fr/FIId/FinInstnId/BICFI", errors);
        requirePresent(context.receiverBic(), "AppHdr/To/FIId/FinInstnId/BICFI", errors);
        requirePresent(context.header().getBusinessMessageIdentifier(),
                "AppHdr/BizMsgIdr", errors);

        String messageDefinition = context.header().getMessageDefinitionIdentifier();
        if (messageDefinition == null || messageDefinition.isBlank()) {
            errors.add(new ValidationError(RejectReasonCode.FF01,
                    "Message definition identifier is missing", "AppHdr/MsgDefIdr"));
        } else if (!EXPECTED_MESSAGE_DEFINITION.equals(messageDefinition)) {
            // The endpoint accepts one message type. Announcing a different one is a
            // routing mistake by the sender and is better reported than guessed at.
            errors.add(new ValidationError(RejectReasonCode.FF01,
                    "Message definition must be " + EXPECTED_MESSAGE_DEFINITION,
                    "AppHdr/MsgDefIdr"));
        }

        if (context.groupHeader() == null) {
            errors.add(new ValidationError(RejectReasonCode.FF01,
                    "Group header is missing", "FIToFICstmrCdtTrf/GrpHdr"));
        } else {
            requirePresent(context.groupHeader().getMessageIdentification(),
                    "GrpHdr/MsgId", errors);
            requirePresent(context.groupHeader().getNumberOfTransactions(),
                    "GrpHdr/NbOfTxs", errors);
        }

        if (context.transactions().isEmpty()) {
            errors.add(new ValidationError(RejectReasonCode.FF01,
                    "Message contains no credit transfer transactions",
                    "FIToFICstmrCdtTrf/CdtTrfTxInf"));
        }

        for (int i = 0; i < context.transactions().size(); i++) {
            validateTransaction(context.transactions().get(i), "CdtTrfTxInf[%d]".formatted(i),
                    errors);
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.rejected(errors);
    }

    private void validateTransaction(CreditTransferTransaction transaction, String path,
                                     List<ValidationError> errors) {
        if (transaction.getPaymentIdentification() == null) {
            errors.add(new ValidationError(RejectReasonCode.FF01,
                    "Payment identification is missing", path + "/PmtId"));
        } else {
            // TxId is optional in ISO 20022 but mandatory here: it is the key on which
            // duplicate detection depends, so a message without one cannot be processed
            // safely no matter how well formed it is.
            requirePresent(transaction.getPaymentIdentification().getTransactionIdentification(),
                    path + "/PmtId/TxId", errors);
            requirePresent(transaction.getPaymentIdentification().getEndToEndIdentification(),
                    path + "/PmtId/EndToEndId", errors);
        }

        if (transaction.getInterbankSettlementAmount() == null) {
            errors.add(new ValidationError(RejectReasonCode.FF01,
                    "Settlement amount is missing", path + "/IntrBkSttlmAmt"));
        } else {
            requirePresent(transaction.getInterbankSettlementAmount().getCurrency(),
                    path + "/IntrBkSttlmAmt/@Ccy", errors);
            if (transaction.getInterbankSettlementAmount().getValue() == null) {
                errors.add(new ValidationError(RejectReasonCode.FF01,
                        "Settlement amount has no value", path + "/IntrBkSttlmAmt"));
            }
        }

        requireAgent(transaction.getDebtorAgent(), path + "/DbtrAgt", errors);
        requireAgent(transaction.getCreditorAgent(), path + "/CdtrAgt", errors);
        requireAccount(transaction.getDebtorAccount(), path + "/DbtrAcct", errors);
        requireAccount(transaction.getCreditorAccount(), path + "/CdtrAcct", errors);
    }

    private void requireAgent(
            com.rbc.paymentvalidation.xml.model.pacs008.BranchAndFinancialInstitutionIdentification
                    agent, String path, List<ValidationError> errors) {
        if (agent == null || agent.bic() == null || agent.bic().isBlank()) {
            errors.add(new ValidationError(RejectReasonCode.FF01,
                    "Agent identification is missing", path + "/FinInstnId/BICFI"));
        }
    }

    private void requireAccount(com.rbc.paymentvalidation.xml.model.pacs008.CashAccount account,
                                String path, List<ValidationError> errors) {
        if (account == null || account.accountNumber() == null
                || account.accountNumber().isBlank()) {
            errors.add(new ValidationError(RejectReasonCode.FF01,
                    "Account identification is missing", path + "/Id"));
        }
    }

    private void requirePresent(String value, String path, List<ValidationError> errors) {
        if (value == null || value.isBlank()) {
            errors.add(new ValidationError(RejectReasonCode.FF01,
                    "Mandatory element is missing", path));
        }
    }
}
