-- V2: Dynamic Multi-Currency Wallet Schema
-- Supports GHS (default), NGN, KES, ZAR, EGP, USD, GBP, EUR with real-time conversion

CREATE TABLE IF NOT EXISTS wallet (
    wallet_id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(customer_id),
    default_currency CHAR(3) NOT NULL DEFAULT 'GHS',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customer_wallet UNIQUE (customer_id)
);

CREATE TABLE IF NOT EXISTS wallet_balance (
    balance_id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES wallet(wallet_id),
    currency CHAR(3) NOT NULL, -- GHS, NGN, KES, ZAR, EGP, USD, GBP, EUR
    available_amount NUMERIC(20,4) NOT NULL DEFAULT 0.0000 CHECK (available_amount >= 0),
    locked_amount NUMERIC(20,4) NOT NULL DEFAULT 0.0000 CHECK (locked_amount >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_wallet_currency UNIQUE (wallet_id, currency)
);

CREATE TABLE IF NOT EXISTS exchange_rate (
    rate_id UUID PRIMARY KEY,
    base_currency CHAR(3) NOT NULL,
    quote_currency CHAR(3) NOT NULL,
    rate NUMERIC(20,6) NOT NULL CHECK (rate > 0),
    inverse_rate NUMERIC(20,6) NOT NULL CHECK (inverse_rate > 0),
    spread_percentage NUMERIC(6,4) NOT NULL DEFAULT 0.0050, -- 0.5% default mock spread
    effective_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_base_quote UNIQUE (base_currency, quote_currency)
);

CREATE TABLE IF NOT EXISTS currency_transfer (
    transfer_id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES wallet(wallet_id),
    from_currency CHAR(3) NOT NULL,
    to_currency CHAR(3) NOT NULL,
    from_amount NUMERIC(20,4) NOT NULL CHECK (from_amount > 0),
    to_amount NUMERIC(20,4) NOT NULL CHECK (to_amount > 0),
    rate_applied NUMERIC(20,6) NOT NULL,
    fee_amount NUMERIC(20,4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    reference VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transfer_wallet ON currency_transfer(wallet_id, created_at DESC);
