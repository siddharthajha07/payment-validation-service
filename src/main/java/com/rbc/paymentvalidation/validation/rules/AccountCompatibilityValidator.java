package com.rbc.paymentvalidation.validation.rules;

import com.rbc.paymentvalidation.domain.Institution;
import com.rbc.paymentvalidation.repository.InstitutionRepository;
import com.rbc.paymentvalidation.validation.PaymentValidator;
import com.rbc.paymentvalidation.validation.RejectReasonCode;
import com.rbc.paymentvalidation.validation.ValidationContext;
import com.rbc.paymentvalidation.validation.ValidationError;
import com.rbc.paymentvalidation.validation.ValidationProperties;
import com.rbc.paymentvalidation.validation.ValidationResult;
import com.rbc.paymentvalidation.xml.model.pacs008.BranchAndFinancialInstitutionIdentification;
import com.rbc.paymentvalidation.xml.model.pacs008.CashAccount;
import com.rbc.paymentvalidation.xml.model.pacs008.CreditTransferTransaction;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Checks that an account number fits the institution holding it, and that transit numbers look
 * right.
 *
 * The brief says institution A uses FI-prefixed accounts and B uses RI. That knowledge lives
 * in the institution table, not here: this rule looks the agent up and asks. Onboarding a
 * third institution is an insert. An institution with no prefix holds no customer accounts and
 * the check does not apply to it.
 *
 * The transit rule is worth naming. The brief requires three digits; the supplied sample
 * carries 05605, five digits, so the sample fails this rule as written. Implemented as
 * specified rather than bent to fit, with the length in configuration and the conflict in
 * ASSUMPTIONS.md.
 *
 * Debtor and creditor accounts must also differ, since a transfer naming one account on both
 * sides moves nothing.
 */
@Component
@Order(40)
public class AccountCompatibilityValidator implements PaymentValidator {

    private final InstitutionRepository institutionRepository;
    private final ValidationProperties properties;

    public AccountCompatibilityValidator(InstitutionRepository institutionRepository,
                                         ValidationProperties properties) {
        this.institutionRepository = institutionRepository;
        this.properties = properties;
    }

    @Override
    public ValidationResult validate(ValidationContext context) {
        List<ValidationError> errors = new ArrayList<>();
        List<CreditTransferTransaction> transactions = context.transactions();

        for (int i = 0; i < transactions.size(); i++) {
            CreditTransferTransaction transaction = transactions.get(i);
            String path = "CdtTrfTxInf[%d]".formatted(i);

            checkAccountAgainstAgent(transaction.getDebtorAgent(), transaction.getDebtorAccount(),
                    path + "/DbtrAcct", errors);
            checkAccountAgainstAgent(transaction.getCreditorAgent(),
                    transaction.getCreditorAccount(), path + "/CdtrAcct", errors);

            checkTransitNumber(transaction.getDebtorAgent(), path + "/DbtrAgt/BrnchId/Id", errors);
            checkTransitNumber(transaction.getCreditorAgent(), path + "/CdtrAgt/BrnchId/Id",
                    errors);

            checkAccountsDiffer(transaction, path, errors);
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.rejected(errors);
    }

    private void checkAccountAgainstAgent(BranchAndFinancialInstitutionIdentification agent,
                                          CashAccount account, String path,
                                          List<ValidationError> errors) {
        Institution institution = institutionRepository.findByBicAndActiveTrue(agent.bic())
                .orElse(null);
        if (institution == null || !institution.holdsCustomerAccounts()) {
            // Either the institution is unknown — already reported by InstitutionValidator,
            // so not repeated here — or it holds no customer accounts and the rule is
            // inapplicable rather than violated.
            return;
        }

        String accountNumber = account.accountNumber();
        if (!accountNumber.startsWith(institution.getAccountPrefix())) {
            errors.add(new ValidationError(RejectReasonCode.AC01,
                    "Account number must begin with %s for institution %s"
                            .formatted(institution.getAccountPrefix(), institution.getBic()),
                    path));
        }
    }

    private void checkTransitNumber(BranchAndFinancialInstitutionIdentification agent,
                                    String path, List<ValidationError> errors) {
        String transitNumber = agent.transitNumber();
        if (transitNumber == null || transitNumber.isBlank()) {
            errors.add(new ValidationError(RejectReasonCode.RC08,
                    "Branch transit number is missing", path));
            return;
        }

        int required = properties.transitNumberLength();
        if (transitNumber.length() != required || !transitNumber.chars().allMatch(
                Character::isDigit)) {
            errors.add(new ValidationError(RejectReasonCode.RC08,
                    "Branch transit number must be exactly %d digits".formatted(required),
                    path));
        }
    }

    private void checkAccountsDiffer(CreditTransferTransaction transaction, String path,
                                     List<ValidationError> errors) {
        String debtorAccount = transaction.getDebtorAccount().accountNumber();
        String creditorAccount = transaction.getCreditorAccount().accountNumber();

        if (debtorAccount.equals(creditorAccount)) {
            errors.add(new ValidationError(RejectReasonCode.AC01,
                    "Debtor and creditor accounts must not be identical",
                    path + "/DbtrAcct and " + path + "/CdtrAcct"));
        }
    }
}
