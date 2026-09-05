-- ==============================================================================
-- Migration: V1__init_user_management_schema.sql
-- Description: Core schema for multi-tenant user management in TexForge
-- ==============================================================================

-- 1. Tenants table
CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Roles table
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Seed initial standard system roles
INSERT INTO roles (name, description) VALUES
    ('SUPER_ADMIN', 'Platform-level super administrator with global system access'),
    ('TENANT_ADMIN', 'Tenant-level organizational administrator'),
    ('EMPLOYEE', 'Standard tenant-level employee');

-- 3. Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    tenant_id UUID NULL REFERENCES tenants(id) ON DELETE RESTRICT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_tenant_email UNIQUE(tenant_id, email)
);

-- Partial unique index for platform users (tenant_id IS NULL)
CREATE UNIQUE INDEX uq_platform_user_email
ON users(email)
WHERE tenant_id IS NULL;

-- Composite index for tenant-aware status filtering
CREATE INDEX idx_users_tenant_status
ON users(tenant_id, status);

-- 4. User Roles junction table
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(user_id, role_id)
);

CREATE INDEX idx_user_roles_role
ON user_roles(role_id);
