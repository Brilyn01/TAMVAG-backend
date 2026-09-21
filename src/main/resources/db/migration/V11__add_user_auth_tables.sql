-- V11: TAMVA User Authentication Tables
-- Adds the user identity layer (separate from partner application auth).
-- tamva_user            : login identity linked to one customer profile.
-- refresh_token_session : server-side refresh token storage (hashed tokens only).

CREATE TABLE IF NOT EXISTS tamva_user (
    user_id       UUID PRIMARY KEY,

    /*
     * Every user must be linked to an existing customer.
     * One customer can have at most one login identity.
     */
    customer_id   UUID NOT NULL
                      REFERENCES customer(customer_id)
                      ON DELETE RESTRICT
                      UNIQUE,

    email         VARCHAR(255) NOT NULL UNIQUE,

    password_hash VARCHAR(255) NOT NULL,

    first_name    VARCHAR(100) NOT NULL,

    last_name     VARCHAR(100) NOT NULL,

    phone_number  VARCHAR(30),

    role          VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER',

    status        VARCHAR(50) NOT NULL DEFAULT 'PENDING_VERIFICATION'
                      CHECK (
                          status IN (
                              'PENDING_VERIFICATION',
                              'ACTIVE',
                              'SUSPENDED',
                              'CLOSED'
                          )
                      ),

    created_at    TIMESTAMPTZ NOT NULL
                      DEFAULT CURRENT_TIMESTAMP,

    updated_at    TIMESTAMPTZ NOT NULL
                      DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tamva_user_email
    ON tamva_user(email);

CREATE INDEX IF NOT EXISTS idx_tamva_user_customer_id
    ON tamva_user(customer_id);


CREATE TABLE IF NOT EXISTS refresh_token_session (
    session_id UUID PRIMARY KEY,

    user_id    UUID NOT NULL
                   REFERENCES tamva_user(user_id)
                   ON DELETE CASCADE,

    token_hash VARCHAR(255) NOT NULL UNIQUE,

    expires_at TIMESTAMPTZ NOT NULL,

    revoked    BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL
                   DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rts_token_hash
    ON refresh_token_session(token_hash);

CREATE INDEX IF NOT EXISTS idx_rts_user_id
    ON refresh_token_session(user_id);