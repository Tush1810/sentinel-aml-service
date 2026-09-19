# Sentinel AML

## 1. What it is

Sentinel is an AML (anti-money-laundering) transaction monitoring prototype. It ingests
customers, accounts, and transactions from a bank's core systems, runs a set of
configurable detection rules against every transaction as it arrives, and raises
risk-scored alerts carrying a human-readable explanation of why each one fired.
Alerts feed an analyst queue for investigation.

## 2. Architecture

The code is layered `resource -> service -> repository`, with one deliberate design
decision: **ingestion is transport-agnostic**. Three adapters (CSV upload, REST, Kafka)
all translate their input into the same `IngestTransactionCommand` and hand it to
`TransactionIngestionService`, which is the only place a transaction is persisted and
scored. No adapter makes an ingestion decision of its own.

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
                                     DetectionEngine
                              (evaluates every enabled rule
                               from rules.yml against the txn)
                                              |
                                              v
                                       AlertService
                              (risk score, severity, dedup)
                                              |
                                              v
                                   alert / alert_evidence
                                              |
                                              v
                                     AlertResource
                              (analyst queue, GET /api/v1/alerts)
```

Package layout under `src/main/java/com/tushar/hackathon/`:

| Package | Role |
|---|---|
| `resource/` | HTTP and Kafka adapters (`resource/ingestion`, `resource/ingestion/kafka`, `resource/alert`) |
| `service/ingestion/` | Transport-neutral ingestion services (customer, account, transaction) + CSV parsing helpers |
| `service/detection/` | `DetectionEngine` (rule evaluation) and `AlertService` (scoring, dedup) |
| `service/` | `ExchangeRateService` (currency normalization), `SentinelProperties` (typed config binding) |
| `repository/` | JPA entities and Spring Data repositories, one sub-package per aggregate (`customer`, `account`, `txn`, `alert`, `amlcase`) |
| `model/response/` | Read-only view/DTO records returned by the resources |
| `exception/` | `ApiException` hierarchy + `GlobalExceptionHandler` mapping to a coded error response |
| `SecurityConfig` | HTTP Basic + role-based route authorization |

Currency normalization happens once, at ingestion (`ExchangeRateService` converts every
amount to a base currency, `amount_base`), so every detection rule compares like with
like and never touches currency conversion itself.

## 3. ERD

Table names use `txn` and `aml_case` instead of `transaction` and `case` because both
are SQL reserved words.

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

Cardinality in words:
- `customer` 1 --- * `account`
- `account` 1 --- * `txn`
- `customer` 1 --- * `alert` (an alert is always tied to a customer; `account_id` is nullable)
- `alert` * --- * `txn` via `alert_evidence` (the transactions that caused the alert)
- `customer` 1 --- * `aml_case`
- `aml_case` * --- * `alert` via `case_alert` (a case bundles the alerts an analyst investigates together)

| Table | Key columns |
|---|---|
| `customer` | `id` PK, `customer_ref` unique, `kyc_status`, `risk_rating`, `politically_exposed` |
| `account` | `id` PK, `account_ref` unique, `customer_id` FK -> customer, `account_status`, `currency`, `current_balance` |
| `txn` | `id` PK, `txn_ref` unique, `account_id` FK -> account, `direction`, `amount`, `currency`, `amount_base`, `exchange_rate`, `counterparty_country`, `txn_timestamp` |
| `alert` | `id` PK, `alert_ref` unique, `customer_id` FK, `account_id` FK (nullable), `rule_code`, `risk_score`, `severity`, `status`, `dedup_key` unique |
| `alert_evidence` | `alert_id` FK, `txn_id` FK — composite PK (`alert_id`, `txn_id`) |
| `aml_case` | `id` PK, `case_ref` unique, `customer_id` FK, `status`, `priority`, `disposition` |
| `case_alert` | `case_id` FK, `alert_id` FK — composite PK (`case_id`, `alert_id`) |

Notable constraints: `alert.risk_score` is checked between 0 and 100; `txn.amount` must
be positive; `alert.dedup_key` is unique and is what actually enforces one-alert-per-pattern
under concurrent evaluation, not application-level locking.

## 4. Setup

### Prerequisites

- Java 21 (`java -version`; on this machine `java_home -v 21` may resolve to the wrong
  JDK — rely on `PATH` instead of overriding `JAVA_HOME`)
- Maven (or use the included `mvnw` if present)
- Docker, for Postgres and Kafka

### Postgres and Kafka

Both run via Docker Compose in a sibling `personal-infra` directory (not part of this
repo):

```
cd ../personal-infra
docker compose up -d
docker ps   # expect personal-infra-postgres-1 and personal-infra-kafka-1
```

The app's default datasource points at database `sentinel` with user/password
`catalog`/`catalog` (see `src/main/resources/application.yml`), matching that
container's setup. Flyway (`V1__create_core_schema.sql`) creates the schema on
first boot (`baseline-on-migrate: true`).

### Environment variables

All have defaults suitable for local development; override for anything else.

| Variable | Default | Purpose |
|---|---|---|
| `SENTINEL_DB_URL` | `jdbc:postgresql://localhost:5432/sentinel` | Postgres JDBC URL |
| `SENTINEL_DB_USER` | `catalog` | Postgres user |
| `SENTINEL_DB_PASSWORD` | `catalog` | Postgres password |
| `SENTINEL_KAFKA_ENABLED` | `true` | Enables the Kafka publish endpoint and listener |
| `SENTINEL_ANALYST_PASSWORD` | `analyst` | Password for the in-memory `analyst` user |
| `SENTINEL_ADMIN_PASSWORD` | `admin` | Password for the in-memory `admin` user |

`KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`) is also read from the environment
by Spring Boot's standard Kafka autoconfiguration property, though it is not one of the
`SENTINEL_*` variables above.

### Build and run

```
mvn clean package
mvn spring-boot:run
# or
java -jar target/hackathon-backend-1.0.0.jar
```

The app listens on port **8081**. Interactive API documentation (springdoc) is served
at `http://localhost:8081/swagger-ui/index.html`, with the raw OpenAPI spec at
`/v3/api-docs` — both are public and need no credentials, while every `/api/v1/**`
route still requires HTTP Basic auth.

## 5. Rule configuration

This is the core of the design: **detection rules live entirely in
`src/main/resources/rules.yml`**, bound at startup into typed records
(`SentinelProperties.Rule`) via `@ConfigurationProperties`. Tuning a threshold, a
window, a weight, or adding a brand-new rule is a YAML edit — no Java code, no
recompilation. `DetectionEngine` reduces every rule to one of four reusable shapes and
switches on `type`:

| Type | What it measures | Rules using it |
|---|---|---|
| `SINGLE_TRANSACTION` | A SpEL `condition` evaluated against the one incoming transaction | `CTR_THRESHOLD`, `HIGH_RISK_JURISDICTION` |
| `WINDOWED_COUNT` | Count (and sum) of transactions matching a `filter` within `window-hours` on the same account | `STRUCTURING`, `ROUND_NUMBER` |
| `INFLOW_OUTFLOW_RATIO` | Ratio of outbound to inbound money on an account within `window-hours` | `RAPID_MOVEMENT` |
| `BASELINE_DEVIATION` | Recent activity vs. a customer's own rolling daily average over `baseline-days` | `BEHAVIORAL_DEVIATION` |

### YAML fields

| Field | Meaning |
|---|---|
| `code` | Unique rule identifier, stored on the alert (`alert.rule_code`) |
| `typology` | Human label for the AML pattern (stored on the alert, shown to analysts) |
| `enabled` | Rule is skipped entirely by `DetectionEngine` when `false` |
| `weight` | Base risk score (0-100) contributed if the rule fires, before customer risk uplifts |
| `type` | One of the four shapes above |
| `scope` | `ACCOUNT` or `CUSTOMER` — which entity the time window is computed over |
| `condition` / `filter` | SpEL expression evaluated against a `Transaction` entity (e.g. `amountBase`, `counterpartyCountry`) |
| `threshold` | Meaning depends on `type`: minimum amount, minimum count, minimum ratio (%), or a multiplier of the baseline |
| `window-hours` | Lookback window size for windowed/ratio/deviation rules |
| `baseline-days` | Historical period used to compute a customer's daily average (`BASELINE_DEVIATION` only) |
| `min-baseline-txns` | Minimum historical transactions required before a baseline is trusted (`BASELINE_DEVIATION` only) |
| `explanation` | Template string rendered with rule-specific placeholders (e.g. `{amount}`, `{account}`, `{count}`) and stored on the alert |

### Worked example, from `rules.yml`

```yaml
- code: STRUCTURING              # unique id, becomes alert.rule_code
  typology: Structuring / Smurfing
  enabled: true                  # flip to false to disable without deleting
  weight: 60                     # base risk score contribution
  type: WINDOWED_COUNT           # count matching txns in a rolling window
  scope: ACCOUNT                 # window is per-account
  window-hours: 24               # look back 24 hours from the triggering txn
  filter: "amountBase >= 9000 and amountBase <= 9999"   # SpEL over each candidate txn
  threshold: 3                   # need >= 3 matching txns in the window to fire
  explanation: "{count} transactions totalling {total} on {account} within {window} hours, each just below the reporting threshold."
```

This detects classic structuring: three or more deposits each just under the $10,000
CTR threshold, within a day, on the same account.

**Rules are read once at application startup.** Editing `rules.yml` requires an
application **restart** to take effect — there is no live/hot reload — but never
requires a code change or a rebuild.

## 6. API

All endpoints require HTTP Basic auth. Two users exist: `analyst` (role `ANALYST`) and
`admin` (role `ADMIN`), passwords from `SENTINEL_ANALYST_PASSWORD` /
`SENTINEL_ADMIN_PASSWORD` (default `analyst` / `admin`).

| Method | Path | Required role | Purpose |
|---|---|---|---|
| `POST` | `/api/v1/ingestion/customers` | ADMIN | Bulk-load customers from a CSV file (`multipart/form-data`, field `file`) |
| `POST` | `/api/v1/ingestion/accounts` | ADMIN | Bulk-load accounts from a CSV file |
| `POST` | `/api/v1/ingestion/transactions` | ADMIN | Bulk-load transactions from a CSV file (runs detection on each accepted row) |
| `POST` | `/api/v1/ingestion/transactions/publish` | ADMIN | Publish a batch of transactions onto the Kafka topic instead of ingesting synchronously (only active when `SENTINEL_KAFKA_ENABLED=true`) |
| `POST` | `/api/v1/transactions` | ADMIN | Ingest one transaction synchronously; returns any alerts it raised |
| `POST` | `/api/v1/transactions/batch` | ADMIN | Ingest a JSON array of transactions synchronously, best-effort (bad rows reported, others still accepted) |
| `GET` | `/api/v1/alerts` | ANALYST or ADMIN | Alert queue, highest risk first; optional `status` and `size` query params. Customer names are masked. |
| `GET` | `/api/v1/alerts/{alertRef}` | ANALYST or ADMIN | Single alert detail. Customer name is masked for ANALYST, full for ADMIN. |
| `GET` | `/swagger-ui/**`, `/swagger-ui.html` | none (public) | Interactive OpenAPI documentation UI |
| `GET` | `/v3/api-docs/**` | none (public) | Raw OpenAPI spec |

### curl examples

Ingest one transaction (ADMIN):

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

Bulk CSV upload of customers (ADMIN):

```
curl -u admin:admin -F "file=@customers.csv" \
  http://localhost:8081/api/v1/ingestion/customers
```

Publish a batch to Kafka instead of ingesting inline (ADMIN):

```
curl -u admin:admin -X POST http://localhost:8081/api/v1/ingestion/transactions/publish \
  -H "Content-Type: application/json" \
  -d '[{"txnRef":"TXN-2001","accountRef":"ACC-0001","direction":"CREDIT","txnType":"DEPOSIT","amount":15000,"currency":"INR","txnTimestamp":"2026-09-19T10:00:00Z"}]'
```

Read the alert queue (ANALYST):

```
curl -u analyst:analyst "http://localhost:8081/api/v1/alerts?status=OPEN&size=20"
```

Read one alert's detail, unmasked (ADMIN):

```
curl -u admin:admin http://localhost:8081/api/v1/alerts/ALT-ABCD1234
```

## 7. Security

Enforced with Spring Security HTTP Basic (`SecurityConfig`), stateless (no session,
CSRF disabled since there is no session cookie to protect). Two in-memory users, one
per role:

| Role | Can do |
|---|---|
| `ANALYST` | `GET /api/v1/alerts/**` — read the alert queue and alert detail (customer names masked) |
| `ADMIN` | Everything `ANALYST` can do, plus all `POST /api/v1/ingestion/**` and `POST /api/v1/transactions/**` endpoints, plus unmasked customer names in alert detail |

Every other `/api/v1/**` request just needs to be authenticated as one of the two
users (`anyRequest().authenticated()`). The exception is the OpenAPI documentation
routes (`/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`), which are explicitly
`permitAll()` — the spec itself is not sensitive, only the data behind it.

Customer name masking (`AlertView`) is done server-side, not hidden client-side: the
alert queue (`AlertView.masked`) always shows `FirstName L.`; only
`GET /api/v1/alerts/{alertRef}` reveals the full `FirstName LastName` and only when the
caller has role `ADMIN`.

## 8. Detection rules implemented

| Code | Typology | Trips when | Business rule satisfied |
|---|---|---|---|
| `CTR_THRESHOLD` | Threshold Reporting | A single transaction's base-currency amount is >= 10,000 | Currency Transaction Report threshold monitoring |
| `HIGH_RISK_JURISDICTION` | High-Risk Jurisdiction | Counterparty country is one of `IR, KP, SY, MM, AF, YE, AE` | Sanctioned/high-risk jurisdiction screening |
| `STRUCTURING` | Structuring / Smurfing | 3+ transactions on an account within 24h, each between 9,000 and 9,999 | Detects deliberate sub-threshold splitting to avoid CTR reporting |
| `ROUND_NUMBER` | Round-Number Pattern | 3+ transactions on an account within 168h (7 days), each >= 10,000 and an exact multiple of 10,000 | Flags suspiciously round, repeated large transfers |
| `RAPID_MOVEMENT` | Rapid Movement of Funds | Outflow is >= 80% of inflow on an account within 48h | Layering: funds not left to rest before moving on |
| `BEHAVIORAL_DEVIATION` | Behavioural Deviation | A customer's activity in the last 24h exceeds 3x their 90-day daily average (min. 5 historical transactions required) | Deviation from established customer behavior baseline |

Each alert's risk score starts at the rule's `weight` and is uplifted for a
politically-exposed customer (+15), HIGH risk rating (+10) or MEDIUM risk rating (+5),
and non-VERIFIED KYC status (+10), capped at 100. Severity bands: CRITICAL >= 85,
HIGH >= 70, MEDIUM >= 50, else LOW.

## 9. Known limitations

- **No live rule reload.** `rules.yml` is bound once at startup via
  `@ConfigurationProperties`; changing it requires an application restart (no code
  change or rebuild needed, but the process must come back up).
- **`RAPID_MOVEMENT` aggregates inflow and outflow independently over the window rather
  than matching each deposit to its corresponding outflow.** Because of this, the
  reported ratio can exceed 100% (e.g. an outflow funded partly by a pre-existing
  balance rather than only the window's inflow) — it is a directional signal, not an
  exact traced-funds ratio.
- **No dead-letter topic on the Kafka path.** `TransactionKafkaListener` logs and drops
  any record it cannot deserialize or ingest; there is no retry topic or DLQ, so a bad
  message is silently lost from the consumer's perspective (only visible in logs).
- **No unit tests.** The `src/test` tree does not exist in this prototype.
- **Alert-to-case workflow is schema-only.** `aml_case` and `case_alert` exist in the
  Flyway migration and as JPA-mapped concepts, but there is no REST resource in this
  build to create, assign, or disposition a case — only the alert queue is exposed.
- Kafka ingestion is fire-and-forget: `POST /api/v1/ingestion/transactions/publish`
  returns `202 Accepted` with just a queued count; there is no way to learn from that
  call whether the message was ever consumed or whether it raised any alerts.
