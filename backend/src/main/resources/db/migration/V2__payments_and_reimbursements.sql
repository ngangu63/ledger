-- Payment and reimbursement tables. These back the @Entity types in the payment
-- and reimbursement packages; with spring.jpa.hibernate.ddl-auto=validate the app
-- will not start unless these columns match the mappings.
--
-- Money is stored as NUMERIC(19,4) (embedded Money -> amount + currency), never
-- as floating point. The authoritative money movement for a payment is its linked
-- ledger_transaction; the rows below track lifecycle/state only.

CREATE TABLE payment (
    id                     UUID          PRIMARY KEY,
    direction              VARCHAR(16)   NOT NULL CHECK (direction IN ('INBOUND','OUTBOUND')),
    method                 VARCHAR(16)   NOT NULL CHECK (method IN ('CARD','BANK')),
    status                 VARCHAR(16)   NOT NULL
                               CHECK (status IN ('PENDING','AUTHORIZED','CAPTURED','SETTLED','FAILED','REVERSED')),
    amount                 NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency               VARCHAR(3)    NOT NULL,
    source_account_id      UUID          NOT NULL REFERENCES account (id),
    destination_account_id UUID          NOT NULL REFERENCES account (id),
    provider               VARCHAR(64),
    provider_ref           VARCHAR(255),
    external_ref           VARCHAR(255),
    transaction_id         UUID          REFERENCES ledger_transaction (id),
    failure_reason         VARCHAR(1024),
    created_at             TIMESTAMPTZ   NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL,
    version                BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX ix_payment_transaction ON payment (transaction_id);

CREATE TABLE reimbursement (
    id                 UUID          PRIMARY KEY,
    requester          VARCHAR(255)  NOT NULL,
    amount             NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency           VARCHAR(3)    NOT NULL,
    description        VARCHAR(1024),
    status             VARCHAR(16)   NOT NULL
                           CHECK (status IN ('SUBMITTED','APPROVED','REJECTED','PAID')),
    funding_account_id UUID          NOT NULL REFERENCES account (id),
    payee_account_id   UUID          NOT NULL REFERENCES account (id),
    decided_by         VARCHAR(255),
    payment_id         UUID          REFERENCES payment (id),
    created_at         TIMESTAMPTZ   NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL,
    version            BIGINT        NOT NULL DEFAULT 0
);
