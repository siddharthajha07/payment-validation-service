package com.rbc.paymentvalidation.validation.rules;

import com.rbc.paymentvalidation.repository.InstitutionRepository;
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
 * Checks that every institution named on the message is one we recognise and that is allowed
 * to participate right now.
 *
 * Active matters as much as known. A suspended participant is flagged rather than deleted so
 * historical payments still resolve, which means looking up by BIC alone would happily accept
 * traffic from a participant that has been suspended.
 *
 * It runs before the account rules because those ask an institution what prefix it uses, and
 * that only has an answer once the institution is known to exist.
 */
@Component
@Order(30)
public class InstitutionValidator implements PaymentValidator {

    private final InstitutionRepository institutionRepository;

    public InstitutionValidator(InstitutionRepository institutionRepository) {
        this.institutionRepository = institutionRepository;
    }

    @Override
    public ValidationResult validate(ValidationContext context) {
        List<ValidationError> errors = new ArrayList<>();

        requireActiveInstitution(context.senderBic(), "AppHdr/Fr", errors);
        requireActiveInstitution(context.receiverBic(), "AppHdr/To", errors);

        List<CreditTransferTransaction> transactions = context.transactions();
        for (int i = 0; i < transactions.size(); i++) {
            String path = "CdtTrfTxInf[%d]".formatted(i);
            CreditTransferTransaction transaction = transactions.get(i);
            requireActiveInstitution(transaction.getDebtorAgent().bic(), path + "/DbtrAgt",
                    errors);
            requireActiveInstitution(transaction.getCreditorAgent().bic(), path + "/CdtrAgt",
                    errors);
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.rejected(errors);
    }

    private void requireActiveInstitution(String bic, String path, List<ValidationError> errors) {
        if (institutionRepository.findByBicAndActiveTrue(bic).isEmpty()) {
            // The message names the BIC, so echoing it back discloses nothing the sender
            // did not already send. It is also the single most useful thing to tell them.
            errors.add(new ValidationError(RejectReasonCode.RC01,
                    "Institution %s is not a recognised active participant".formatted(bic),
                    path));
        }
    }
}
