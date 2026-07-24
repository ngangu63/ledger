-- Ledger core schema: accounts, transactions, postings, balances, idempotency.
-- Design principles enforced here:
--   * money stored as NUMERIC (never floating point)
--   * postings are append-only (no UPDATE/DELETE expected in application code)
--   * a partial guarantee of double-entry integrity via CHECK + application logic

CREATE TABLE account (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(16)  NOT NULL CHECK (type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')),
    currency    VARCHAR(3)   NOT NULL,
    status      VARCHAR(16)  NOT NULL CHECK (status IN ('ACTIVE','FROZEN','CLOSED')),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE ledger_transaction (
    id             UUID         PRIMARY KEY,
    external_ref   VARCHAR(255),
    description    VARCHAR(1024),
    status         VARCHAR(16)  NOT NULL CHECK (status IN ('POSTED','REVERSED')),
    reversal_of_id UUID         REFERENCES ledger_transaction (id),
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    version        BIGINT       NOT NULL DEFAULT 0
);

-- External references, when supplied, must be unique so callers can rely on them
-- as a natural idempotency anchor in addition to the Idempotency-Key header.
CREATE UNIQUE INDEX ux_ledger_transaction_external_ref
    ON ledger_transaction (external_ref)
    WHERE external_ref IS NOT NULL;

CREATE TABLE posting (
    id             UUID         PRIMARY KEY,
    transaction_id UUID         NOT NULL REFERENCES ledger_transaction (id),
    account_id     UUID         NOT NULL REFERENCES account (id),
    direction      VARCHAR(8)   NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
    amount         NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency       VARCHAR(3)   NOT NULL
);

CREATE INDEX ix_posting_account ON posting (account_id);
CREATE INDEX ix_posting_transaction ON posting (transaction_id);

CREATE TABLE account_balance (
    account_id    UUID          PRIMARY KEY REFERENCES account (id),
    signed_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency      VARCHAR(3)     NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL,
    updated_at    TIMESTAMPTZ   NOT NULL,
    version       BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE idempotency_record (
    idempotency_key VARCHAR(200) PRIMARY KEY,
    scope           VARCHAR(64)  NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    resource_id     UUID         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL
);
