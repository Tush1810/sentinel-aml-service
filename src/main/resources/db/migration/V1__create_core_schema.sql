-- Sentinel AML core schema: customer -> account -> txn, and the alert/case workflow.
-- Table names avoid the SQL reserved words "transaction" and "case".

CREATE TABLE customer (
    id                       BIGSERIAL PRIMARY KEY,
    customer_ref             VARCHAR(32)  NOT NULL UNIQUE,
    first_name               VARCHAR(100) NOT NULL,
    last_name                VARCHAR(100) NOT NULL,
    gender                   VARCHAR(16),
    date_of_birth            DATE,
    email                    VARCHAR(160),
    phone_number             VARCHAR(32),
    city                     VARCHAR(100),
    state                    VARCHAR(100),
    country                  VARCHAR(2),
    postal_code              VARCHAR(16),
    occupation               VARCHAR(100),
    annual_income            NUMERIC(18, 2),
    marital_status           VARCHAR(32),
    education_level          VARCHAR(64),
    employment_status        VARCHAR(32),
    customer_since           DATE,
    customer_segment         VARCHAR(32),
    kyc_status               VARCHAR(16)  NOT NULL,
    risk_rating              VARCHAR(16)  NOT NULL,
    politically_exposed      BOOLEAN      NOT NULL DEFAULT FALSE,
    preferred_channel        VARCHAR(48),
    email_verified           BOOLEAN      NOT NULL DEFAULT FALSE,
    phone_verified           BOOLEAN      NOT NULL DEFAULT FALSE,
    num_complaints_last_year INTEGER      NOT NULL DEFAULT 0,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE account (
    id                       BIGSERIAL PRIMARY KEY,
    account_ref              VARCHAR(32) NOT NULL UNIQUE,
    customer_id              BIGINT      NOT NULL REFERENCES customer (id),
    account_type             VARCHAR(32) NOT NULL,
    account_status           VARCHAR(16) NOT NULL,
    currency                 VARCHAR(3)  NOT NULL,
    open_date                DATE,
    close_date               DATE,
    branch_code              VARCHAR(16),
    branch_city              VARCHAR(100),
    current_balance          NUMERIC(18, 2),
    avg_monthly_balance_6m   NUMERIC(18, 2),
    credit_limit             NUMERIC(18, 2),
    credit_utilization_pct   NUMERIC(6, 2),
    overdraft_enabled        BOOLEAN     NOT NULL DEFAULT FALSE,
    card_type                VARCHAR(32),
    joint_account            BOOLEAN     NOT NULL DEFAULT FALSE,
    num_linked_devices       INTEGER,
    mobile_banking_enrolled  BOOLEAN     NOT NULL DEFAULT FALSE,
    last_login_date          DATE,
    avg_monthly_txn_count    INTEGER,
    account_tier             VARCHAR(32),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_account_customer ON account (customer_id);

CREATE TABLE txn (
    id                    BIGSERIAL PRIMARY KEY,
    txn_ref               VARCHAR(48)    NOT NULL UNIQUE,
    account_id            BIGINT         NOT NULL REFERENCES account (id),
    direction             VARCHAR(8)     NOT NULL,
    txn_type              VARCHAR(32)    NOT NULL,
    amount                NUMERIC(18, 2) NOT NULL,
    currency              VARCHAR(3)     NOT NULL,
    amount_base           NUMERIC(18, 2) NOT NULL,
    exchange_rate         NUMERIC(18, 6) NOT NULL,
    counterparty_name     VARCHAR(160),
    counterparty_account  VARCHAR(48),
    counterparty_bank     VARCHAR(160),
    counterparty_country  VARCHAR(2),
    channel               VARCHAR(32),
    description           VARCHAR(255),
    txn_timestamp         TIMESTAMPTZ    NOT NULL,
    ingested_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT chk_txn_amount_positive CHECK (amount > 0)
);

-- Detection rules scan an account's recent history on every transaction; without this
-- index the windowed rules degrade to a full scan per transaction.
CREATE INDEX idx_txn_account_time ON txn (account_id, txn_timestamp DESC);
CREATE INDEX idx_txn_time         ON txn (txn_timestamp DESC);
CREATE INDEX idx_txn_country      ON txn (counterparty_country);

CREATE TABLE alert (
    id            BIGSERIAL PRIMARY KEY,
    alert_ref     VARCHAR(48) NOT NULL UNIQUE,
    customer_id   BIGINT      NOT NULL REFERENCES customer (id),
    account_id    BIGINT      REFERENCES account (id),
    rule_code     VARCHAR(48) NOT NULL,
    typology      VARCHAR(64) NOT NULL,
    risk_score    INTEGER     NOT NULL,
    severity      VARCHAR(16) NOT NULL,
    status        VARCHAR(24) NOT NULL,
    explanation   VARCHAR(2000) NOT NULL,
    -- One pattern must yield one alert even when two threads evaluate the same account
    -- concurrently; the unique constraint is what actually enforces de-duplication.
    dedup_key     VARCHAR(160) NOT NULL UNIQUE,
    detected_at   TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_alert_risk_score CHECK (risk_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_alert_customer ON alert (customer_id);
CREATE INDEX idx_alert_queue    ON alert (status, risk_score DESC);

-- Supporting evidence: the transactions that caused the alert to fire.
CREATE TABLE alert_evidence (
    alert_id BIGINT NOT NULL REFERENCES alert (id) ON DELETE CASCADE,
    txn_id   BIGINT NOT NULL REFERENCES txn (id),
    PRIMARY KEY (alert_id, txn_id)
);

CREATE TABLE aml_case (
    id                 BIGSERIAL PRIMARY KEY,
    case_ref           VARCHAR(48) NOT NULL UNIQUE,
    customer_id        BIGINT      NOT NULL REFERENCES customer (id),
    status             VARCHAR(24) NOT NULL,
    priority           VARCHAR(16) NOT NULL,
    assigned_to        VARCHAR(64),
    disposition        VARCHAR(32),
    disposition_reason VARCHAR(1000),
    disposed_by        VARCHAR(64),
    disposed_at        TIMESTAMPTZ,
    opened_at          TIMESTAMPTZ NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_case_customer ON aml_case (customer_id);
CREATE INDEX idx_case_status   ON aml_case (status);

-- A case bundles the alerts an analyst investigates as one decision.
CREATE TABLE case_alert (
    case_id  BIGINT NOT NULL REFERENCES aml_case (id) ON DELETE CASCADE,
    alert_id BIGINT NOT NULL REFERENCES alert (id),
    PRIMARY KEY (case_id, alert_id)
);
