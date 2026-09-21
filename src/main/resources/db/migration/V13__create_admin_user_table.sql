CREATE TABLE admin_user (
    admin_user_id UUID PRIMARY KEY,

    email VARCHAR(255) NOT NULL,

    password_hash VARCHAR(255) NOT NULL,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100) NOT NULL,

    role VARCHAR(50) NOT NULL DEFAULT 'ADMIN',

    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_admin_user_email
        UNIQUE (email),

    CONSTRAINT chk_admin_user_role
        CHECK (role IN ('ADMIN', 'SUPER_ADMIN')),

    CONSTRAINT chk_admin_user_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

CREATE INDEX idx_admin_user_status
    ON admin_user (status);