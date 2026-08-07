# Assumptions

Decisions taken where the brief was silent, ambiguous, or contradicted the sample data.
Each one names what was assumed and why.

## 1. The missing appendices

The brief references *Appendix B — Required HTTP headers* and *Appendix C — Additional
business validation rules*, but the document ends at those two headings with no content
beneath them. Only Appendix A (the XML samples) was supplied. On asking, the recruiter said
to fill them in as I saw fit and record the assumptions. Both are therefore invented, and
deliberately kept to rules that are simple to state and simple to justify.

### Appendix B, as assumed

| Header | Required | Purpose |
|---|---|---|
| `Content-Type: application/xml` | yes | Payload type. A mismatch returns 415. |
| `X-Idempotency-Key` | yes | Identifies a submission so a retry can be recognised. |
| `X-Sender-Institution` | yes | The BIC the caller claims to send as. Checked against `AppHdr/Fr`. |
| `X-Correlation-Id` | no | Client trace id. Generated when absent, always echoed back. |

### Appendix C, as assumed

| Rule | Reject code |
|---|---|
| Amount greater than zero | `AM01` |
| Amount not negative, at most two decimal places | `AM02` |
| Currency must be BBD | `AM03` |
| Group total equals the sum of the transactions | `AM09` |
| `NbOfTxs` equals the actual transaction count | `AM18` |
| Settlement date not in the past, at most two days ahead | `DT01` |
| Debtor and creditor accounts must differ | `AC01` |
| Institution BIC must be known and active | `RC01` |

All reject codes are published ISO 20022 External Status Reason codes rather than private
inventions, because the receiving bank's software matches on them.

## 2. The supplied BICs are not valid ISO BICs

The ISO pattern is `[A-Z0-9]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3}){0,1}` — eight or eleven
characters, with letters in positions five and six. None of the three BICs in the sample
conform:

| BIC | Length | Problem |
|---|---|---|
| `BANKA000` | 8 | digit in position six, where ISO requires a letter |
| `CBANK0IPS` | 9 | ISO permits only eight or eleven |
| `BANKB000` | 8 | digit in position six |

These are clearly clearing-system test identifiers. Applying the strict pattern would have
rejected the sample the assessment itself supplies, so the schemas relax it to
`[A-Z0-9]{8,11}`. A production deployment would restore the ISO pattern once the test data
conformed.

## 3. The three-digit transit rule contradicts the sample

The brief requires a three-digit transit number. The sample carries `BrnchId/Id` of `05605`,
which is five digits, so the sample fails the rule as written.

The rule is implemented as specified rather than bent to fit the data. The required length
is held in configuration (`payment.validation.transit-number-length`), so if the intent was
five digits it is a configuration change and not a code change. A test,
`AccountCompatibilityValidatorTest.rejectsSuppliedSampleTransitNumbers`, asserts that the
supplied sample is rejected with `RC08` — the conflict is stated in code, not just here.

## 4. The sample identifies both ultimate parties as the same customer

The sample carries organisation identifier `6075857` on both `UltmtDbtr` (named
`PYRAMID ENT MAN INC`) and `UltmtCdtr` (named `test`). Since the identifier is the customer
key, one customer record results and the party processed second supplies the name — so the
message describes a party paying itself.

This is treated as test data rather than a rule to enforce. The account compatibility rule
compares accounts, not customers, so the message is otherwise valid. Customer names are
last-write-wins for the same reason.

## 5. XML handling

- **The XSDs are hand-authored subsets**, not the official ISO schemas. Generating from the
  full pacs.008 produces several hundred types, almost all unused, which cannot be reviewed
  or explained. The trade-off: a message using ISO-valid elements outside the subset is
  rejected.
- **Inbound signature verification is out of scope.** The brief asks that the response be
  signed, not that the request be verified. The inbound signature is parsed and retained as
  an intact DOM element so verification could be added without changing the model.
- **`OrgnlTxRef` is omitted from responses.** It is optional in ISO 20022 and repeats data
  the sender already holds.
- **Only `pacs.008.001.12` is accepted.** `MsgDefIdr` is checked, and a message announcing a
  different type is rejected rather than guessed at.

## 6. Persistence

- **`ddl-auto: create-drop`** generates the schema from the entities at startup. Acceptable
  for an embedded database in an assessment; production would use Flyway or Liquibase so
  schema changes are versioned and reviewable.
- **Institution reference data is inlined** in `InstitutionSeeder`. Production would read the
  scheme's participant registry, since institutions join, leave and are suspended without a
  release.
- **`CBANK0IPS` is seeded with no account prefix.** It is a valid counterparty on the
  business header but never a debtor or creditor agent, so the account compatibility rule
  does not apply to it.
- **Rejected payments are stored, but update no customer data.** A sender asking why their
  payment did not arrive is asking about a rejected row, so those must be queryable. But a
  message that failed validation is not a trustworthy source of truth about a customer.
- **The audit trail is append-only within the application**, enforced three ways: no setters,
  every column `updatable = false`, and a repository that declares no delete operation. None
  of that stops someone issuing `UPDATE audit_event` directly against the database. Real
  tamper-evidence needs revoked database grants, an append-only store, or hash-chaining.

## 7. Security

- **The signing keystore is a self-signed test key committed to the repository**, with its
  password in `application.yml` in plain text. This is so the service runs straight from a
  clone. Production would hold the private key in an HSM or a secrets manager and present a
  certificate issued by a CA the counterparty trusts. Because these are configuration values,
  pointing at such a keystore is a deployment change.
- **`X-Sender-Institution` against `AppHdr/Fr` is not authentication.** It catches a
  misconfigured client. It does not stop a determined impersonator, because the caller
  supplies both values. Production would establish identity from a mutual-TLS client
  certificate and check the header against that.

## 8. Configuration rather than constants

The supported currency, permitted decimal places, transit number length and settlement
window all live in `application.yml`. They are policy, not logic: the currency changes with
the clearing system, the settlement window is a scheme decision, and the transit length is a
national convention. Recompiling to change any of them would be the wrong shape of answer.

## 9. Running the sample

The supplied sample settles on `2026-07-31`, which is now in the past and is therefore
rejected with `DT01` against the real clock. Tests use a fixed clock so they do not expire.
For manual use, `scripts/submit-sample.sh` stamps today's date before posting.

The conformant fixture `pacs008-valid.xml` differs from the supplied sample in exactly four
values — two account numbers gaining their institution prefixes, and two transit identifiers
reduced to three digits. Everything else is byte-for-byte as provided.
