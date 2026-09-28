-- Update the initial TAMVA super administrator account.

UPDATE admin_user
SET
    email = 'superadmin@tamva.com',
    role = 'SUPER_ADMIN',
    operational_role = 'SECURITY_ADMINISTRATOR',
    updated_at = CURRENT_TIMESTAMP
WHERE admin_user_id = '00000000-0000-0000-0000-000000000001';