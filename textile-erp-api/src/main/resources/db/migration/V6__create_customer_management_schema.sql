-- ==============================================================================
-- Migration: V6__create_customer_management_schema.sql
-- Description: Schema for Customer Management and Shipping Parties
-- ==============================================================================

-- 1. Customers table
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    gstin VARCHAR(15),
    pan VARCHAR(10),
    phone VARCHAR(50),
    email VARCHAR(255),

    -- Billing address
    billing_address_line1 VARCHAR(255),
    billing_address_line2 VARCHAR(255),
    billing_city VARCHAR(100),
    billing_district VARCHAR(100),
    billing_state VARCHAR(100),
    billing_state_code VARCHAR(10),
    billing_pincode VARCHAR(20),
    billing_country VARCHAR(100) DEFAULT 'India',

    payment_terms_days INTEGER DEFAULT 30,
    credit_limit NUMERIC(15,2) DEFAULT 0.00,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_customers_tenant_code UNIQUE(tenant_id, customer_code)
);

CREATE INDEX idx_customers_tenant_id ON customers(tenant_id);
CREATE INDEX idx_customers_tenant_code ON customers(tenant_id, customer_code);
CREATE INDEX idx_customers_tenant_gstin ON customers(tenant_id, gstin);
CREATE INDEX idx_customers_tenant_name ON customers(tenant_id, name);

-- 2. Shipping Parties table (reusable shipping party / destination addresses)
CREATE TABLE shipping_parties (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    gstin VARCHAR(15),
    pan VARCHAR(10),
    phone VARCHAR(50),
    email VARCHAR(255),

    -- Shipping destination address
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    district VARCHAR(100),
    state VARCHAR(100),
    state_code VARCHAR(10),
    pincode VARCHAR(20),
    country VARCHAR(100) DEFAULT 'India',

    same_as_billing BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_shipping_parties_tenant_id ON shipping_parties(tenant_id);
CREATE INDEX idx_shipping_parties_customer_id ON shipping_parties(customer_id);
CREATE INDEX idx_shipping_parties_tenant_gstin ON shipping_parties(tenant_id, gstin);
