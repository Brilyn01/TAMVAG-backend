-- V4: Transaction ingestion quarantine

CREATE TABLE IF NOT EXISTS transaction_quarantine (
    quarantine_id UUID PRIMARY KEY,
    connection_id UUID NOT NULL REFERENCES connection(connection_id),
    source_event_id VARCHAR(255),
    source_system VARCHAR(100),
    account_ref_token VARCHAR(255),
    reason TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    raw_payload TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_quarantine_connection
    ON transaction_quarantine(connection_id);

CREATE INDEX IF NOT EXISTS idx_quarantine_created_at
    ON transaction_quarantine(created_at);

CREATE INDEX IF NOT EXISTS idx_quarantine_status
    ON transaction_quarantine(status);