-- V5: Versioned customer feature snapshots

CREATE TABLE IF NOT EXISTS feature_snapshot (
    feature_snapshot_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(customer_id),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    feature_version VARCHAR(50) NOT NULL,

    total_inflows NUMERIC(20,4) NOT NULL DEFAULT 0,
    total_outflows NUMERIC(20,4) NOT NULL DEFAULT 0,
    net_cash_flow NUMERIC(20,4) NOT NULL DEFAULT 0,

    transaction_count BIGINT NOT NULL DEFAULT 0,
    inflow_transaction_count BIGINT NOT NULL DEFAULT 0,
    outflow_transaction_count BIGINT NOT NULL DEFAULT 0,

    average_inflow NUMERIC(20,4) NOT NULL DEFAULT 0,
    average_outflow NUMERIC(20,4) NOT NULL DEFAULT 0,

    largest_inflow NUMERIC(20,4) NOT NULL DEFAULT 0,
    largest_outflow NUMERIC(20,4) NOT NULL DEFAULT 0,

    income_consistency NUMERIC(20,8) NOT NULL DEFAULT 0,
    expense_consistency NUMERIC(20,8) NOT NULL DEFAULT 0,

    computed_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_feature_snapshot_customer_period_version
        UNIQUE (
            customer_id,
            period_start,
            period_end,
            feature_version
        ),

    CONSTRAINT chk_feature_snapshot_period
        CHECK (period_end >= period_start),

    CONSTRAINT chk_feature_snapshot_counts
        CHECK (
            transaction_count >= 0
            AND inflow_transaction_count >= 0
            AND outflow_transaction_count >= 0
        )
);

CREATE INDEX IF NOT EXISTS idx_feature_snapshot_customer
    ON feature_snapshot(customer_id);

CREATE INDEX IF NOT EXISTS idx_feature_snapshot_period
    ON feature_snapshot(period_start, period_end);

