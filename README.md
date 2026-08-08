# Payment Validation Service

Accepts an ISO 20022 **pacs.008** customer credit transfer over HTTP, validates it against
schema and business rules, records the payment and the customers it names, writes an
immutable audit trail, and returns a digitally signed **pacs.002** status report.

Built for the RBC Senior Software Developer assessment.

- [ARCHITECTURE.md](ARCHITECTURE.md) — how it fits together, one page
- [ASSUMPTIONS.md](ASSUMPTIONS.md) — decisions taken where the brief was silent or the
  sample data contradicted it

---

## Requirements

- **Java 17**
- **Maven 3.9+**

Check with `java -version` and `mvn -version`.

If Java 17 is installed alongside other versions and is not the default — for example a
Homebrew install, which is keg-only — point at it explicitly for the session:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home)
```

## Build and run

```bash
mvn clean verify      # compiles, runs 174 unit and 21 integration tests
mvn spring-boot:run   # starts on http://localhost:8080
```

The service is ready when the log shows:

```
Validation chain initialised with 8 rules: [MandatoryFieldsValidator, ...]
Loaded signing certificate for CN=CBANK0IPS-SIGNING
Tomcat started on port 8080
Seeded institution BANKA000 with account prefix FI
```

Or run the packaged jar:

```bash
java -jar target/payment-validation-service-1.0.0.jar
```

## Trying it

The quickest way is the supplied script, which posts the sample and prints the response:

```bash
scripts/submit-sample.sh                    # accepted, 200 + signed ACCP
scripts/submit-sample.sh --replay           # run twice: 200 + X-Idempotent-Replay: true
scripts/submit-sample.sh --duplicate        # run twice: 422 + AM05
scripts/submit-sample.sh --reject-currency  # 422 + AM03
scripts/submit-sample.sh --reject-parties   # 422 + AGNT
scripts/submit-sample.sh --original         # the sample exactly as supplied: 422
```

**Why a script rather than a plain curl command.** The supplied sample settles on
2026-07-31, which is now in the past, and the settlement date rule rejects backdated
payments. Rather than weaken the rule or ship a sample that quietly stops working, the
script stamps today's date before posting. The same reasoning is in
[ASSUMPTIONS.md](ASSUMPTIONS.md).

By hand, if you prefer:

```bash
sed "s|<IntrBkSttlmDt>2026-07-31</IntrBkSttlmDt>|<IntrBkSttlmDt>$(date -u +%Y-%m-%d)</IntrBkSttlmDt>|" src/test/resources/samples/pacs008-valid.xml > /tmp/payment.xml

curl -i -X POST http://localhost:8080/api/v1/payments -H 'Content-Type: application/xml' -H 'X-Idempotency-Key: my-first-payment' -H 'X-Sender-Institution: BANKA000' --data-binary @/tmp/payment.xml
```

## API

### `POST /api/v1/payments`

**Request headers**

| Header | Required | Purpose |
|---|---|---|
| `Content-Type: application/xml` | yes | Payload type |
| `X-Idempotency-Key` | yes | Identifies this submission so a retry is recognised |
| `X-Sender-Institution` | yes | BIC the caller sends as; must match `AppHdr/Fr` |
| `X-Correlation-Id` | no | Trace id; generated if absent, always echoed back |

**Response headers**

| Header | Meaning |
|---|---|
| `X-Correlation-Id` | Ties this request to its log lines and audit events |
| `X-Idempotent-Replay` | `true` when the body was replayed from an earlier identical request |

**Status codes**

| Status | Meaning | Body |
|---|---|---|
| 200 | Accepted, or a replay of an earlier answer | signed pacs.002 |
| 422 | Understood but rejected on a business rule | signed pacs.002 with an ISO reason code |
| 400 | Malformed XML, schema violation, missing header | error document |
| 409 | Idempotency key reused with a different payload | error document |
| 415 | Content type is not `application/xml` | error document |
| 500 | Unexpected fault | error document with correlation id only |

A rejection returns a **signed pacs.002**, not an error document — the counterparty needs a
document it can authenticate and act on. Error documents are reserved for requests that
never became payments, where a status report could not quote the identifiers it requires.

### Validation rules, in order

| Rule | Enforces | Reject code |
|---|---|---|
| `MandatoryFieldsValidator` | every element this service needs is present | `FF01` |
| `SenderReceiverValidator` | sender and receiver differ; header matches payload | `AGNT` |
| `InstitutionValidator` | BICs are known and active | `RC01` |
| `AccountCompatibilityValidator` | account prefix matches institution, transit format, accounts differ | `AC01`, `RC08` |
| `AmountValidator` | positive, at most two decimals, supported currency | `AM01`, `AM02`, `AM03` |
| `ControlTotalsValidator` | `NbOfTxs` and group total match reality | `AM18`, `AM09` |
| `SettlementDateValidator` | within the settlement window | `DT01` |
| `DuplicateValidator` | transaction not already processed | `AM05` |

The chain stops at the first failure: later rules assume earlier ones passed, and a pacs.002
carries one reason at group level.

## Inspecting the data

The H2 console is enabled at **http://localhost:8080/h2-console**

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:mem:paymentdb` |
| User | `sa` |
| Password | *(blank)* |

```sql
SELECT * FROM PAYMENT;
SELECT * FROM AUDIT_EVENT ORDER BY OCCURRED_AT;
SELECT * FROM CUSTOMER;
```

Every row written by one request shares a correlation id, which also appears on the response
header and on every log line for that request.

## Tests

```bash
mvn test      # 174 unit tests, a few seconds
mvn verify    # adds 21 integration tests
```

Unit tests are `*Test` and run under Surefire; integration tests are `*IT` and run under
Failsafe after packaging, so the inner development loop stays fast.

Worth looking at: `SecureXmlParserTest` fires real XXE and billion-laughs payloads at the
parser, `XmlSignatureServiceTest` proves a signature survives serialisation and stops
verifying once a byte changes, and `AuditEventRepositoryTest` tampers with a stored event by
reflection to prove the change never reaches the database.

## Layout

```
src/main/java/com/rbc/paymentvalidation/
├── api/          controller, global exception handler, error document
├── config/       clock, institution seed data
├── domain/       JPA entities
├── logging/      correlation id filter, masking
├── mapper/       message to entity, and the pacs.002 factory
├── repository/   Spring Data interfaces
├── service/      processing pipeline, customers, audit, idempotency
├── validation/   the rule interface, the chain, and the eight rules
└── xml/          secure parsing, schema validation, binding, signing
    └── model/    JAXB classes for the envelope, header, pacs.008 and pacs.002

src/main/resources/
├── xsd/          three hand-authored subset schemas
└── keystore/     test-only signing key (see ASSUMPTIONS.md)
```

## Requirements traceability

| Requirement | Where |
|---|---|
| Java 17, Spring Boot 3.x, Maven | `pom.xml` |
| REST API, XML request and response | `api/PaymentController` |
| Embedded database | H2, `application.yml` |
| JUnit 5 | `src/test`, 195 tests |
| Accept pacs.008 with required headers | `api/PaymentController` |
| Validate XML structure and mandatory fields | `xml/XsdValidator`, `validation/rules/MandatoryFieldsValidator` |
| Sender and receiver must not be identical | `validation/rules/SenderReceiverValidator` |
| Institution and account compatibility, transit number | `validation/rules/AccountCompatibilityValidator` |
| Detect duplicate requests | `service/IdempotencyService`, `validation/rules/DuplicateValidator`, unique index on `payment.transaction_id` |
| Create or update customers and accounts | `service/CustomerService` |
| Persistence model | `domain/`, six tables — see ARCHITECTURE.md |
| Digitally signed pacs.002 | `mapper/Pacs002Factory`, `xml/signature/XmlSignatureService` |
| Meaningful status codes and business errors | `service/ProcessingOutcome`, `api/GlobalExceptionHandler` |
| Clean code, SOLID | `validation/PaymentValidator` chain — adding a rule changes no existing file |
| Global exception handling | `api/GlobalExceptionHandler` |
| Structured logging with correlation IDs | `logging/CorrelationIdFilter`, `logback-spring.xml` |
| No sensitive data or payloads logged | `logging/MaskingUtil` |
| Protection against XXE and malformed XML | `xml/SecureXmlParser` |
| Unit and integration tests | 174 unit, 21 integration |
| Branching strategy, branches retained, merged to main | `git log --graph --oneline --all` |

## Git

One feature branch per unit of work, merged into `main` with `--no-ff` so the branch
structure stays visible in the history. No branch is deleted. All work is merged to `main`.

```bash
git log --graph --oneline --all
```

## Troubleshooting

**A 422 with `DT01` when posting the sample by hand.** The sample's settlement date is in the
past. Use `scripts/submit-sample.sh`, or stamp the date as shown above — note that must be
the **UTC** date (`date -u`), not the local one. The service compares against
`Clock.systemUTC()`, since a settlement date is a scheme date rather than a property of
whichever machine is running the service. West of Greenwich in the evening the local date is
already yesterday in UTC, and a payment dated yesterday is correctly refused as backdated.

**A 422 with `AM05` on a second run.** Duplicate detection is working: the transaction id has
already been processed. The script generates a fresh one each run.

**A 415.** The `Content-Type: application/xml` header is missing. If you pasted a multi-line
curl command, check the backslashes are at line ends rather than followed by a space.

**A 400 with `MISSING_REQUIRED_HEADER`.** `X-Idempotency-Key` or `X-Sender-Institution` was
not supplied.

**`mvn` builds against the wrong Java version.** Set `JAVA_HOME` as shown under Requirements.
