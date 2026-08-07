# Architecture

A Spring Boot service that accepts an ISO 20022 pacs.008 credit transfer over HTTP,
validates it, records the payment and the customers it names, writes an immutable audit
trail, and returns a digitally signed pacs.002 status report.

## Request flow

```
POST /api/v1/payments  (application/xml in, application/xml out)
  CorrelationIdFilter        one trace id on every log line, row and response header
  PaymentController          reads the body as bytes; no business logic
  IdempotencyService         a replay is answered from store before any parsing
  SecureXmlParser            DOCTYPE refused, so XXE and entity expansion die here
  XsdValidator               structure, order, cardinality, lexical form
  Pacs008Unmarshaller        XML to objects
  PaymentValidationService   eight business rules, first failure wins
  PaymentRecordingService    customers, accounts, payment, audit events
  Pacs002Factory             ACCP or RJCT with an ISO reason code
  XmlSignatureService        enveloped XMLDSig, RSA-SHA256
  GlobalExceptionHandler     wraps everything the above cannot answer
```

## The decision that shapes everything else

**A business rejection is not an error.** A payment that breaks a rule still receives a
well-formed, signed pacs.002 carrying `GrpSts=RJCT` and a published ISO reason code, because
that document is what the counterparty bank acts on. Exceptions are reserved for cases where
a status report is impossible — a payload that will not parse, a missing header, an internal
fault — since a status report must quote identifiers that were never read.

| Outcome | Status | Body |
|---|---|---|
| Accepted | 200 | signed pacs.002, ACCP |
| Business rule failed | 422 | signed pacs.002, RJCT with reason code |
| Repeat of an answered request | 200 | the stored response, `X-Idempotent-Replay: true` |
| Idempotency key reused with a different payload | 409 | error document |
| Malformed XML, missing header | 400 | error document |
| Wrong content type | 415 | error document |
| Unexpected fault | 500 | error document, correlation id only |

## Persistence

Six tables, each answering one of the purposes the brief names.

| Table | Serves | Key decision |
|---|---|---|
| `institution` | reference data | The FI/RI account prefix is a column, so onboarding a third institution is an insert, not a release |
| `customer` | customer maintenance | Keyed on the organisation identifier, never the name; `@Version` guards concurrent updates |
| `account` | customer maintenance | Unique on number *and* institution, since two banks may issue the same number |
| `payment` | transaction lookup, reconciliation | Unique `transaction_id` makes duplicate detection a database guarantee, not an application hope; rejections stored too |
| `idempotency_record` | duplicate detection | Stores the response so a replay is byte-identical; hashes the request so payload data is not retained |
| `audit_event` | auditing, troubleshooting | Append-only; events commit in their own transaction so a rollback cannot erase the record of the attempt |

## Security

External entity resolution is disabled at the parser, which defeats both XXE and
billion-laughs; the payload size is capped before parsing. Schemas are compiled from the
classpath with external access forbidden, so no `schemaLocation` is ever fetched. Logs and
the audit trail carry message identifiers and masked account numbers only — never customer
names, never payloads. Error responses disclose no stack trace, exception type or internal
path, only a correlation id.

## Extending it

Adding a validation rule is one class implementing `PaymentValidator`, one `@Order`
annotation, and one test. `PaymentValidationService` receives every rule from Spring as a
sorted list and knows nothing about which exist, so no existing file changes.
