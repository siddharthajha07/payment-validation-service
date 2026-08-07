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
 * Confirms that every institution named on the message is one this service recognises and
 * that is currently permitted to participate.
 *
 * <h2>Why "active" and not merely "known"</h2>
 * A suspended participant is marked inactive rather than deleted, so that historical
 * payments continue to reference a row that still exists. It follows that recognising a
 * BIC is not sufficient — the institution must also be active right now. Looking up only
 * by BIC would happily accept traffic from a participant that has been suspended.
 *
 * <p>Running before the account rules is deliberate: those rules ask an institution what
 * account prefix it uses, and that question only has an answer once the institution is
 * known to exist.
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
