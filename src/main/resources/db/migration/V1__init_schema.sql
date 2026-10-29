-- Accounts
CREATE TABLE accounts (
    id              VARCHAR(64)     PRIMARY KEY,
    balance         DECIMAL(19, 2)  NOT NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Ledger entries
CREATE TABLE transactions (
    id                  VARCHAR(64)     PRIMARY KEY,
    account_id          VARCHAR(64)     NOT NULL REFERENCES accounts(id),
    type                VARCHAR(32)     NOT NULL,
    amount              DECIMAL(19, 2)  NOT NULL,
    timestamp           TIMESTAMP WITH TIME ZONE NOT NULL,
    description         VARCHAR(255),
    related_account_id  VARCHAR(64),
    CONSTRAINT chk_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_timestamp ON transactions(timestamp);

-- Idempotency keys (scoped by operation + key)
CREATE TABLE idempotency_keys (
    idempotency_key     VARCHAR(128)    NOT NULL,
    operation           VARCHAR(64)     NOT NULL,
    request_hash        VARCHAR(64)     NOT NULL,
    response_status     INT             NOT NULL,
    response_body       TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (idempotency_key, operation)
);

CREATE INDEX idx_idempotency_created ON idempotency_keys(created_at);

-- Transactional Outbox
CREATE TABLE outbox_events (
    id              BIGSERIAL       PRIMARY KEY,
    aggregate_type  VARCHAR(64)     NOT NULL,
    aggregate_id    VARCHAR(64)     NOT NULL,
    event_type      VARCHAR(128)    NOT NULL,
    payload         TEXT            NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMP WITH TIME ZONE,
    status          VARCHAR(32)     NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_outbox_status_created ON outbox_events(status, created_at);
