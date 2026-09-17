-- V6: TAMVA Webhook Delivery Infrastructure
-- Partner webhook subscriptions and durable delivery attempts

CREATE TABLE IF NOT EXISTS webhook_subscription (
    webhook_id UUID PRIMARY KEY,
    application_id UUID NOT NULL REFERENCES application(application_id),
    url VARCHAR(2048) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    events TEXT NOT NULL,
    signing_secret_ciphertext TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_webhook_subscription_status
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_webhook_subscription_application
    ON webhook_subscription(application_id);

CREATE INDEX IF NOT EXISTS idx_webhook_subscription_status
    ON webhook_subscription(status);


CREATE TABLE IF NOT EXISTS webhook_delivery (
    delivery_id UUID PRIMARY KEY,
    webhook_id UUID NOT NULL REFERENCES webhook_subscription(webhook_id),
    event_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,

    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempt_at TIMESTAMPTZ,

    response_status INTEGER,
    response_body TEXT,
    last_error TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,

    CONSTRAINT chk_webhook_delivery_status
        CHECK (
            status IN (
                'PENDING',
                'DELIVERING',
                'DELIVERED',
                'FAILED',
                'EXHAUSTED'
            )
        ),

    CONSTRAINT chk_webhook_delivery_attempts
        CHECK (attempt_count >= 0),

    CONSTRAINT uq_webhook_delivery_event
        UNIQUE (webhook_id, event_id)
);

CREATE INDEX IF NOT EXISTS idx_webhook_delivery_pending
    ON webhook_delivery(status, next_attempt_at);

CREATE INDEX IF NOT EXISTS idx_webhook_delivery_webhook
    ON webhook_delivery(webhook_id);

CREATE INDEX IF NOT EXISTS idx_webhook_delivery_event
    ON webhook_delivery(event_id);

CREATE INDEX IF NOT EXISTS idx_webhook_delivery_created
    ON webhook_delivery(created_at DESC);