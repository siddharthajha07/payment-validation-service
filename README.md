# Payment Validation Service

An ISO 20022 payment validation service that accepts a **pacs.008** customer credit transfer
over HTTP, validates it, persists the business data, maintains customer records, records an
immutable audit trail, and returns a digitally signed **pacs.002** status report.

Built for the RBC Senior Software Developer assessment.

> Full setup, architecture and assumptions documentation is added in a later phase.

## Requirements

- Java 17
- Maven 3.9+

## Build and run

```bash
mvn clean verify
mvn spring-boot:run
```
