-- ==============================================================================
-- Migration: V3__seed_superadmin_user.sql
-- Description: Seed initial default SuperAdmin user in database
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
    '018e0000-0000-7000-8000-000000000001',
    NULL,
    'admin@gmail.com',
    '$2a$10$ditLalwB.fQZseOW2T9KEuIl4S8FF7ZnpcheLclu/HsIQgFuo8WKe',
    'Super',
    'Admin',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) WHERE tenant_id IS NULL DO NOTHING;

INSERT INTO user_roles (user_id, role_id, assigned_at)
SELECT u.id, r.id, CURRENT_TIMESTAMP
FROM users u, roles r
WHERE u.email = 'admin@gmail.com'
  AND u.tenant_id IS NULL
  AND r.name = 'SUPER_ADMIN'
ON CONFLICT (user_id, role_id) DO NOTHING;
