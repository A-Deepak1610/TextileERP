-- ==============================================================================
-- Migration: V4__seed_new_superadmin_user.sql
-- Description: Seed new default SuperAdmin credentials in database
-- ==============================================================================

INSERT INTO users (
    id,
    tenant_id,
    email,
    password_hash,
    first_name,
    last_name,
    status,
    created_at,
    updated_at
) VALUES (
    '018e0000-0000-7000-8000-000000000003',
    NULL,
    'admin@texforge.com',
    '$2a$10$HsnzBbp0Qn1f59gPQqCp3.DaKY0nLqUmDxSNCayoxl.LqFDvXCi9m',
    'Super',
    'Admin',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) WHERE tenant_id IS NULL DO UPDATE 
SET password_hash = EXCLUDED.password_hash,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.email = 'admin@texforge.com'
  AND u.tenant_id IS NULL
  AND r.name = 'SUPER_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
