ALTER TABLE idempotency_keys
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED';

ALTER TABLE idempotency_keys
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE idempotency_keys SET status = 'COMPLETED', updated_at = created_at;

ALTER TABLE idempotency_keys
    ALTER COLUMN response_status DROP NOT NULL;