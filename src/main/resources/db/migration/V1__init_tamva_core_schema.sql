-- V1: TAMVA Core Schema
-- Financial Trust & Identity Infrastructure

CREATE TABLE IF NOT EXISTS customer (
    customer_id UUID PRIMARY KEY,
    external_ref VARCHAR(100),
    type VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL',
    status VARCHAR(50) NOT NULL CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED')),
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS institution (
    institution_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- BANK, DEMI, PSP, LENDER, FINTECH
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    regulatory_reference VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS application (
    application_id UUID PRIMARY KEY,
    institution_id UUID NOT NULL REFERENCES institution(institution_id),
    client_id VARCHAR(100) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    scopes TEXT, -- JSON array string of scopes
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS connection (
    connection_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(customer_id),
    institution_id UUID NOT NULL REFERENCES institution(institution_id),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    provider_ref VARCHAR(255),
    last_sync_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS consent (
    consent_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(customer_id),
    institution_id UUID NOT NULL REFERENCES institution(institution_id),
    purpose VARCHAR(255) NOT NULL,
    scopes TEXT NOT NULL, -- JSON array of scopes
    status VARCHAR(50) NOT NULL CHECK (status IN ('REQUESTED', 'ACTIVE', 'SUSPENDED', 'REVOKED', 'EXPIRED')),
    granted_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS account (
    account_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(customer_id),
    institution_id UUID NOT NULL REFERENCES institution(institution_id),
    account_type VARCHAR(50) NOT NULL, -- MOBILE_MONEY, BANK_CURRENT, SAVINGS, WALLET
    currency CHAR(3) NOT NULL,
    account_ref_token VARCHAR(255),
    masked_identifier VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transaction (
    transaction_id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES account(account_id),
    source_event_id VARCHAR(255) NOT NULL,
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('IN', 'OUT')),
    amount NUMERIC(20,4) NOT NULL CHECK (amount >= 0),
    currency CHAR(3) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) NOT NULL, -- RECEIVED, NORMALISED, DUPLICATE, PENDING, COMPLETED, FAILED, REVERSED
    channel VARCHAR(50),
    counterparty TEXT, -- JSON representation
    merchant_category VARCHAR(100),
    reference VARCHAR(255),
    source_system VARCHAR(100),
    normalisation_version VARCHAR(50) NOT NULL DEFAULT '1.0.0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_account_source_event UNIQUE (account_id, source_event_id)
);

CREATE INDEX IF NOT EXISTS idx_tx_account_occurred ON transaction(account_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS ledger_entry (
    ledger_entry_id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES transaction(transaction_id),
    entry_type VARCHAR(50) NOT NULL, -- INCOME, EXPENSE, TRANSFER, SAVING, DEBT, INVESTMENT, FEE, OTHER
    amount NUMERIC(20,4) NOT NULL,
    currency CHAR(3) NOT NULL,
    balance_effect VARCHAR(20) NOT NULL, -- DEBIT, CREDIT
    balance_after NUMERIC(20,4),
    counterparty_id VARCHAR(255),
    confidence NUMERIC(5,4) DEFAULT 1.0000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS feature (
    feature_id UUID PRIMARY KEY,
    subject_id UUID NOT NULL,
    subject_type VARCHAR(50) NOT NULL, -- CUSTOMER, ACCOUNT
    feature_name VARCHAR(100) NOT NULL,
    feature_value TEXT NOT NULL,
    window_period VARCHAR(50) NOT NULL, -- 5m, 1h, 24h, 7d, 30d, 90d, 180d, 365d, REAL_TIME
    feature_version VARCHAR(50) NOT NULL DEFAULT '1.0.0',
    computed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS risk_event (
    risk_event_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(customer_id),
    account_id UUID REFERENCES account(account_id),
    transaction_id UUID,
    request_id VARCHAR(100),
    risk_score INT NOT NULL,
    decision VARCHAR(50) NOT NULL, -- ALLOW, CHALLENGE, HOLD, BLOCK
    recommended_action VARCHAR(100),
    reason_codes TEXT, -- JSON array of string reason codes
    ruleset_version VARCHAR(50),
    model_version VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'EVALUATED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_risk_event_cust_created ON risk_event(customer_id, created_at DESC);

CREATE TABLE IF NOT EXISTS passport (
    passport_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(customer_id),
    passport_type VARCHAR(50) NOT NULL, -- LENDING_PROFILE, BUSINESS_PROFILE, FINANCIAL_SUMMARY, PAYMENT_TRUST_PROFILE, INTERNATIONAL_PROFILE
    purpose VARCHAR(255) NOT NULL,
    data_categories TEXT NOT NULL, -- JSON array of categories
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    version VARCHAR(50) NOT NULL DEFAULT '1.0',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS passport_share (
    share_id UUID PRIMARY KEY,
    passport_id UUID NOT NULL REFERENCES passport(passport_id),
    recipient_id UUID NOT NULL REFERENCES institution(institution_id),
    purpose VARCHAR(255) NOT NULL,
    scopes TEXT NOT NULL, -- JSON array
    share_token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    shared_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS device (
    device_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(customer_id),
    fingerprint_token VARCHAR(255) NOT NULL,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trust_state VARCHAR(50) NOT NULL DEFAULT 'TRUSTED'
);

CREATE TABLE IF NOT EXISTS beneficiary (
    beneficiary_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(customer_id),
    token VARCHAR(255) NOT NULL,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS case_record (
    case_id UUID PRIMARY KEY,
    risk_event_id UUID NOT NULL REFERENCES risk_event(risk_event_id),
    severity VARCHAR(50) NOT NULL, -- LOW, MEDIUM, HIGH, CRITICAL
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- OPEN, TRIAGED, INVESTIGATING, ACTIONED, RESOLVED, ESCALATED
    source VARCHAR(50) NOT NULL DEFAULT 'RULES_ENGINE',
    assignee VARCHAR(100),
    disposition VARCHAR(50), -- CONFIRMED_RISK, FALSE_POSITIVE, CUSTOMER_CONFIRMED, OTHER
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS audit_event (
    audit_id UUID PRIMARY KEY,
    actor_type VARCHAR(50) NOT NULL,
    actor_id VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    correlation_id VARCHAR(100),
    event_hash VARCHAR(64) NOT NULL,
    payload TEXT
);

CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_event(timestamp DESC);
