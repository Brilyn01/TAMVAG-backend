-- Seed the initial TAMVA SUPER_ADMIN account.
--
-- IMPORTANT:
-- Replace the password_hash value below with a BCrypt hash
-- generated from the intended bootstrap password before deployment.

INSERT INTO admin_user (
    admin_user_id,
    email,
    password_hash,
    first_name,
    last_name,
    role,
    status,
    created_at,
    updated_at
)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@tamva.com',
    '$2a$10$hHZbeKZqFEp0i3/3YQNG4OYtEjrb1UXwgJubgRY.Hv220PrvjsN.u',
    'TAMVA',
    'Administrator',
    'SUPER_ADMIN',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;