-- Banking integration: bank transfers (ACH / wire) settle asynchronously.
-- A transfer is recorded in the ledger at initiation (money in transit) and
-- reaches a terminal state via a settlement callback (SETTLED) or a return
-- (RETURNED), the latter posting an offsetting ledger entry.

CREATE TABLE bank_transfer (
    id                     UUID          PRIMARY KEY,
    rail                   VARCHAR(16)   NOT NULL CHECK (rail IN ('ACH','WIRE')),
    amount                 NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency               VARCHAR(3)    NOT NULL,
    source_account_id      UUID          NOT NULL REFERENCES account (id),
    destination_account_id UUID          NOT NULL REFERENCES account (id),
    status                 VARCHAR(16)   NOT NULL
                               CHECK (status IN ('PENDING','SETTLED','RETURNED')),
    provider               VARCHAR(64),
    provider_ref           VARCHAR(255),
    external_ref           VARCHAR(255),
    transaction_id         UUID          REFERENCES ledger_transaction (id),
    settled_at             TIMESTAMPTZ,
    created_at             TIMESTAMPTZ   NOT NULL,
    updated_at             TIMESTAMPTZ   NOT NULL,
    version                BIGINT        NOT NULL DEFAULT 0
);

-- Settlement callbacks look transfers up by the provider's reference.
CREATE INDEX ix_bank_transfer_provider_ref ON bank_transfer (provider_ref);
