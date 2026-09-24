ALTER TABLE admin_user
    ADD COLUMN operational_role VARCHAR(50);

ALTER TABLE admin_user
    ADD CONSTRAINT chk_admin_user_operational_role
    CHECK (
        operational_role IS NULL
        OR operational_role IN (
            'RISK_ANALYST',
            'SECURITY_ADMINISTRATOR',
            'PLATFORM_OPERATOR'
        )
    );

CREATE INDEX idx_admin_user_operational_role
    ON admin_user (operational_role);