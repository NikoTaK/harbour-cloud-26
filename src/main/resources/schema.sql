-- Run against every shard at startup (see DataSourceConfig). Kept simple and
-- idempotent so it is safe to re-run on each boot.

CREATE TABLE IF NOT EXISTS bulk_requests (
    id              UUID PRIMARY KEY,
    status          VARCHAR(20)  NOT NULL,
    total_items     INTEGER      NOT NULL,
    processed_items INTEGER      NOT NULL,
    created_at      TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS bulk_payment_items (
    id              UUID PRIMARY KEY,
    bulk_request_id UUID         NOT NULL,
    store_id        VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128),
    coffee_type     VARCHAR(32)  NOT NULL,
    price           NUMERIC(12,2) NOT NULL,
    currency        VARCHAR(3)   NOT NULL,
    loyalty_card_id VARCHAR(128),
    status          VARCHAR(20)  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_items_request ON bulk_payment_items (bulk_request_id);
