# Sentinel AML

## 1. What this service does

Sentinel is an AML (anti-money-laundering) transaction monitoring prototype. This service
ingests customers, accounts, and transactions from a bank's core systems, then serves the
resulting risk-scored alerts to an analyst queue. An analyst bundles alerts into a case and
records one decision for that case. Detection itself lives in a sibling repo,
`sentinel-aml-engine`. That engine consumes Debezium change events for the `txn` table from
Kafka, evaluates the rule book, and writes the alerts back.

## 2. Architecture: one ingestion path, detection elsewhere

The code is layered `resource -> service -> repository`, around one deliberate design
decision: **ingestion is transport-agnostic**. Three adapters (CSV upload, REST, Kafka)
translate their input into the same `IngestTransactionCommand` and hand it to
`TransactionIngestionService`, the only place a transaction is persisted. No adapter makes
an ingestion decision of its own.

```
                    CSV upload            REST POST             Kafka topic
                (IngestionResource)  (TransactionResource)  (TransactionKafkaListener)
                        |                     |                       |
                        v                     v                       v
                        +---------------------+-----------------------+
                                              |
                                 IngestTransactionCommand
                                              |
                                              v
                              TransactionIngestionService
                              (currency normalization, persist)
                                              |
                                              v
                                            txn
                                              |
                                              v
                            Debezium change events on Kafka
                                              |
                                              v
                                   sentinel-aml-engine
                                (evaluates the rule book)
                                              |
                                              v
                                 alert and alert_evidence
                                              |
                                              v
                                       AlertResource
                              (analyst queue, GET /api/v1/alerts)
                                              |
                                              v
                                       CaseResource
                          (open, assign, dispose, POST /api/v1/cases)
```

The table below lists every package and root class under
`src/main/java/com/tushar/sentinel/`.

| Package or class | Role |
|---|---|
| `resource/` | HTTP and Kafka adapters: `resource/ingestion`, `resource/ingestion/kafka`, `resource/alert`, `resource/casemanagement`, `resource/dashboard` |
| `service/ingestion/` | Transport-neutral ingestion services for customers, accounts, and transactions, plus the CSV helpers `CsvFile` and `CsvValues` |
| `service/casemanagement/` | `CaseService`, which runs the investigation workflow and writes the audit trail |
| `service/` | `ExchangeRateService` for currency normalization, `SentinelProperties` for typed config binding |
| `repository/` | JPA entities and Spring Data repositories, one sub-package per aggregate: `customer`, `account`, `txn`, `alert`, `amlcase`, `auditlog` |
| `model/response/` | Read-only records the resources return, grouped as `alert`, `casemanagement`, `dashboard`, `ingestion` |
| `common/rest/response/` | `ErrorResponse`, the error body every failing endpoint returns |
| `exception/` | The `ApiException` hierarchy and `GlobalExceptionHandler`, which maps each exception to an `ErrorResponse` carrying an `ErrorCode` |
| `SecurityConfig` | HTTP Basic auth and role-based route authorization |
| `SentinelApplication` | Spring Boot entry point |

`ExchangeRateService` converts every amount to the base currency once, at ingestion, and
stores the result in `amount_base`. The detection rules downstream therefore compare like
with like and never handle currency conversion themselves. The base currency and the rate
table live under `sentinel.currency` in `src/main/resources/application.yml`.

## 3. Data model: seven core tables and an append-only audit log

The tables are named `txn` and `aml_case` rather than `transaction` and `case`, because both
of those are SQL reserved words.

```
 customer  1 ---- * account  1 ---- * txn
    |                                  |
    | 1                                | * (evidence)
    |                                  |
    * alert  *------------* alert_evidence
    |  |
    |  | *
    |  case_alert
    |  |
    * (via case_alert)      *
    aml_case ----------------
       (customer 1 ---- * aml_case)
```

Read the cardinality as follows.

- `customer` 1 --- * `account`
- `account` 1 --- * `txn`
- `customer` 1 --- * `alert`. An alert always belongs to a customer, and `account_id` is nullable.
- `alert` * --- * `txn` through `alert_evidence`, the transactions that caused the alert.
- `customer` 1 --- * `aml_case`
- `aml_case` * --- * `alert` through `case_alert`. A case bundles the alerts an analyst investigates as one decision.
- `audit_log` has no foreign key to either table. Each row points at a case or an alert by the text column `entity_ref`.

| Table | Key columns |
|---|---|
| `customer` | `id` PK, `customer_ref` unique, `kyc_status`, `risk_rating`, `politically_exposed` |
| `account` | `id` PK, `account_ref` unique, `customer_id` FK -> customer, `account_status`, `currency`, `current_balance` |
| `txn` | `id` PK, `txn_ref` unique, `account_id` FK -> account, `direction`, `amount`, `currency`, `amount_base`, `exchange_rate`, `counterparty_country`, `txn_timestamp` |
| `alert` | `id` PK, `alert_ref` unique, `customer_id` FK, `account_id` FK (nullable), `rule_code`, `typology`, `risk_score`, `severity`, `status`, `explanation`, `dedup_key` unique |
| `alert_evidence` | `alert_id` FK, `txn_id` FK, composite PK (`alert_id`, `txn_id`) |
| `aml_case` | `id` PK, `case_ref` unique, `customer_id` FK, `status`, `priority`, `assigned_to`, `disposition`, `disposition_reason`, `disposed_by`, `disposed_at` |
| `case_alert` | `case_id` FK, `alert_id` FK, composite PK (`case_id`, `alert_id`) |
| `audit_log` | `id` PK, `entity_type`, `entity_ref`, `from_status`, `to_status`, `actor`, `reason`, `occurred_at` |

Three constraints carry design weight. A check keeps `alert.risk_score` between 0 and 100.
A second check keeps `txn.amount` positive. The unique constraint on `alert.dedup_key` is
what enforces one alert per pattern when two evaluations of the same account run at once. No
application-level lock does that work.

`audit_log` is append-only in the database, not only in application code. The trigger
`trg_audit_log_append_only` raises an exception on any `UPDATE` or `DELETE`. An auditor can
therefore trust the trail even when something other than this service holds a connection.

## 4. Set up the infrastructure, then the app

### Prerequisites

You need three things before you build.

- Java 21. Check with `java -version`. On this machine `java_home -v 21` resolves to the wrong JDK, so rely on `PATH` instead of overriding `JAVA_HOME`.
- Maven. This repo ships no Maven wrapper, so use the `mvn` on your `PATH`.
- Docker, for Postgres and Kafka.

### Start Postgres and Kafka

Postgres and Kafka run as Docker containers defined outside this repo. Two Compose files
named `personal-infra` exist, and only one of them defines a broker.

- `/Users/tusharpruthi/Desktop/Practice/Hackathon/examples-staging/personal-infra/docker-compose.yml` defines both a `postgres` service and a `kafka` service. Start both from here.
- `/Users/tusharpruthi/Desktop/Practice/Examples/personal-infra/docker-compose.yml` defines `postgres` only. Bringing that file up gives you a database but no broker, and `TransactionKafkaListener` then fails to reach `localhost:9092`.

Both files sit in a directory named `personal-infra`, so Compose derives the same project
name and the same container names from each: `personal-infra-postgres-1` and
`personal-infra-kafka-1`. They also share the named volume `hackathon-postgres-data`. Treat
the two files as one project. A Compose command run in one directory can act on a container
started from the other.

To start the database and the broker together, use the `examples-staging` copy.

```
cd /Users/tusharpruthi/Desktop/Practice/Hackathon/examples-staging/personal-infra
docker compose up -d
docker ps   # expect personal-infra-postgres-1 and personal-infra-kafka-1
```

The Compose file creates the database `catalog` with user `catalog` and password `catalog`.
It does not create the `sentinel` database this service points at. To create that database
on a fresh volume, run the following command once.

```
docker exec personal-infra-postgres-1 psql -U catalog -d postgres -c "CREATE DATABASE sentinel"
```

Flyway then runs on first boot with `baseline-on-migrate: true` and applies
`V1__create_core_schema.sql`, `V2__seed_synthetic_data.sql`, and `V3__create_audit_log.sql`.

### Environment variables

Every variable below has a default that suits local development. Override them anywhere else.

| Variable | Default | Purpose |
|---|---|---|
| `SENTINEL_DB_URL` | `jdbc:postgresql://localhost:5432/sentinel` | Postgres JDBC URL |
| `SENTINEL_DB_USER` | `catalog` | Postgres user |
| `SENTINEL_DB_PASSWORD` | `catalog` | Postgres password |
| `SENTINEL_KAFKA_ENABLED` | `true` | Registers the Kafka publish endpoint, the publisher, and the listener |
| `SENTINEL_ANALYST_PASSWORD` | `analyst` | Password for the in-memory `analyst` user |
| `SENTINEL_ADMIN_PASSWORD` | `admin` | Password for the in-memory `admin` user |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Broker address |

`KAFKA_BOOTSTRAP_SERVERS` feeds Spring Boot's standard `spring.kafka.bootstrap-servers`
property. It is the one variable in the table that carries no `SENTINEL_` prefix.

### Build and run

Build the jar, then start the app either through Maven or from the jar.

```
mvn clean package
mvn spring-boot:run
# or
java -jar target/sentinel-aml-service-1.0.0.jar
```

The app listens on port **8081**. springdoc serves interactive API documentation at
`http://localhost:8081/swagger-ui/index.html` and the raw OpenAPI spec at `/v3/api-docs`.
Both are public and need no credentials. Every `/api/v1/**` route still requires HTTP Basic
auth.

## 5. Detection runs in sentinel-aml-engine, not here

This service holds no detection logic. The rule book, and the engine that evaluates it, live
in the sibling repo `sentinel-aml-engine`. That engine consumes Debezium change events for
the `txn` table from Kafka and writes the alerts this service serves. Thresholds, windows, and
weights are configured there, not in this repo's `application.yml`.

## 6. API endpoints and the roles they need

Every `/api/v1/**` endpoint requires HTTP Basic auth. Two users exist: `analyst` with role
`ANALYST`, and `admin` with role `ADMIN`. Their passwords come from
`SENTINEL_ANALYST_PASSWORD` and `SENTINEL_ADMIN_PASSWORD`, defaulting to `analyst` and
`admin`.

| Method | Path | Required role | Purpose |
|---|---|---|---|
| `POST` | `/api/v1/ingestion/customers` | ADMIN | Load customers from a CSV file (`multipart/form-data`, field `file`) |
| `POST` | `/api/v1/ingestion/accounts` | ADMIN | Load accounts from a CSV file |
| `POST` | `/api/v1/ingestion/transactions` | ADMIN | Load transactions from a CSV file |
| `POST` | `/api/v1/ingestion/transactions/publish` | ADMIN | Publish transactions onto the Kafka topic `sentinel.transactions` instead of ingesting them inline. Returns `202 Accepted` with a queued count. Registered only when `SENTINEL_KAFKA_ENABLED` is `true`. |
| `POST` | `/api/v1/transactions` | ADMIN | Ingest one transaction. Returns `201 Created` and an `IngestResult` of `txnRef` and `status`. It reports no alerts, because detection runs elsewhere. |
| `POST` | `/api/v1/transactions/batch` | ADMIN | Ingest a JSON array of transactions. The call is best-effort: bad rows come back in `BatchResult.errors()`, and the rest are still accepted. |
| `GET` | `/api/v1/alerts` | ANALYST or ADMIN | Alert queue, highest risk first. Optional `status` and `size` query params. Customer names are masked. |
| `GET` | `/api/v1/alerts/{alertRef}` | ANALYST or ADMIN | One alert. ADMIN sees the full customer name, ANALYST sees the masked name. |
| `POST` | `/api/v1/cases` | ANALYST or ADMIN | Open a case over `alertRefs` at a `priority`. Returns `201 Created`. The alerts must all belong to one customer. |
| `POST` | `/api/v1/cases/{caseRef}/assignment` | ANALYST or ADMIN | Assign the case to the `assignee` query param and move it to `IN_REVIEW` |
| `POST` | `/api/v1/cases/{caseRef}/disposition` | ANALYST or ADMIN | Record a `disposition` and a `reason`, then close or escalate the case and its alerts |
| `GET` | `/api/v1/cases` | ANALYST or ADMIN | Case queue, with optional `status` and `size` query params |
| `GET` | `/api/v1/cases/{caseRef}` | ANALYST or ADMIN | One case, including its disposition and the analyst who made it |
| `GET` | `/api/v1/cases/{caseRef}/audit` | ANALYST or ADMIN | Audit trail for the case and every alert it bundles, oldest first |
| `GET` | `/api/v1/dashboard/summary` | ANALYST or ADMIN | Headline counts, plus alert counts by severity, by rule, and by status |
| `GET` | `/api/v1/dashboard/customers` | ANALYST or ADMIN | Customers with their account refs and their transaction and alert counts, highest risk score first |
| `GET` | `/api/v1/dashboard/customers/{customerRef}/transactions` | ANALYST or ADMIN | One customer's transaction timeline |
| `GET` | `/swagger-ui/**`, `/swagger-ui.html` | none (public) | Interactive OpenAPI documentation |
| `GET` | `/v3/api-docs/**` | none (public) | Raw OpenAPI spec |

A case moves through `OPEN`, `IN_REVIEW`, then either `CLOSED` or `ESCALATED_TO_SAR`. The
disposition that closes it is one of `FALSE_POSITIVE`, `CLEARED`, or `ESCALATED_TO_SAR`.
`CaseService` takes the actor for every transition from the authenticated principal, never
from the request body, so a caller cannot forge the audit trail.

### How to call the API with curl

To ingest one transaction as ADMIN, run the following command.

```
curl -u admin:admin -X POST http://localhost:8081/api/v1/transactions \
  -H "Content-Type: application/json" \
  -d '{
        "txnRef": "TXN-1001",
        "accountRef": "ACC-0001",
        "direction": "DEBIT",
        "txnType": "WIRE",
        "amount": 9500,
        "currency": "USD",
        "counterpartyCountry": "AE",
        "txnTimestamp": "2026-09-19T10:00:00Z"
      }'
```

The response confirms acceptance and nothing more.

```
{"txnRef":"TXN-1001","status":"ACCEPTED"}
```

To bulk-load customers from a CSV file as ADMIN, post the file on the `file` field.

```
curl -u admin:admin -F "file=@customers.csv" \
  http://localhost:8081/api/v1/ingestion/customers
```

To publish a batch to Kafka instead of ingesting it inline, call the publish endpoint as
ADMIN.

```
curl -u admin:admin -X POST http://localhost:8081/api/v1/ingestion/transactions/publish \
  -H "Content-Type: application/json" \
  -d '[{"txnRef":"TXN-2001","accountRef":"ACC-0001","direction":"CREDIT","txnType":"DEPOSIT","amount":15000,"currency":"INR","txnTimestamp":"2026-09-19T10:00:00Z"}]'
```

To read the alert queue as ANALYST, filter by status and cap the page size.

```
curl -u analyst:analyst "http://localhost:8081/api/v1/alerts?status=OPEN&size=20"
```

To read one alert with the customer name unmasked, call it as ADMIN.

```
curl -u admin:admin http://localhost:8081/api/v1/alerts/ALT-ABCD1234
```

To open a case over two alerts as ANALYST, post their refs with a priority.

```
curl -u analyst:analyst -X POST http://localhost:8081/api/v1/cases \
  -H "Content-Type: application/json" \
  -d '{"alertRefs":["ALT-ABCD1234","ALT-EF567890"],"priority":"HIGH"}'
```

To close that case, post a disposition and the reason behind it.

```
curl -u analyst:analyst -X POST http://localhost:8081/api/v1/cases/CASE-1A2B3C4D/disposition \
  -H "Content-Type: application/json" \
  -d '{"disposition":"FALSE_POSITIVE","reason":"Salary credit from a known employer"}'
```

## 7. Security: HTTP Basic, two roles, masking on the server

`SecurityConfig` enforces HTTP Basic auth over a stateless filter chain. It disables CSRF
protection, because a stateless API issues no session cookie for CSRF to protect. Two
in-memory users exist, one per role.

| Role | Can do |
|---|---|
| `ANALYST` | Read the alert queue and alert detail with customer names masked. Read the dashboard. Open, assign, and dispose cases, and read their audit trails. |
| `ADMIN` | Everything `ANALYST` can do, plus every `POST /api/v1/ingestion/**` and `POST /api/v1/transactions/**` endpoint, plus unmasked customer names in alert detail. |

Any other `/api/v1/**` request needs only an authenticated user, through
`anyRequest().authenticated()`. The OpenAPI documentation routes are the exception:
`/swagger-ui/**`, `/swagger-ui.html`, and `/v3/api-docs/**` are explicitly `permitAll()`.
The spec itself is not sensitive. Only the data behind it is.

`AlertView` masks customer names on the server rather than hiding them in a client. The
queue always uses `AlertView.masked`, which renders `FirstName L.`. Only
`GET /api/v1/alerts/{alertRef}` calls `AlertView.full` for the complete
`FirstName LastName`, and only when the caller holds role `ADMIN`.

## 8. Known limitations

- **The Kafka path has no dead-letter topic.** `TransactionKafkaListener` catches every exception, logs `Skipping unprocessable Kafka record`, and moves on. There is no retry topic and no DLQ, so a malformed payload leaves no trace outside the log.
- **Testing stops at CSV parsing.** `src/test/java/com/tushar/sentinel/service/ingestion/CsvFileTest.java` holds two tests over `CsvFile`. One checks that a rejected row is reported against its own line number while the remaining rows still load. The other checks that a row whose column count does not match the header is rejected instead of parsed. Nothing else in the service has a test.
- **Kafka ingestion is fire-and-forget.** `POST /api/v1/ingestion/transactions/publish` returns `202 Accepted` with a queued count. That response tells you nothing about whether a consumer ever handled the message.
- **Two hard-coded users stand in for a user directory.** `SecurityConfig` builds an `InMemoryUserDetailsManager` with `analyst` and `admin`. Every analyst therefore shares one login, and `audit_log.actor` records `analyst` for all of their decisions.
- **The dashboard aggregates in memory.** `DashboardResource.summary()` and `DashboardResource.customers()` both call `alertRepository.findAll()` and group the results in Java. That holds at prototype volumes. Push the grouping into SQL before the `alert` table grows large.
