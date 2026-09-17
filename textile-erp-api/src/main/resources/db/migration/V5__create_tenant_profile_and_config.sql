-- ==============================================================================
-- Migration: V5__create_tenant_profile_and_config.sql
-- Description: Schema for Tenant Profile & Business Configuration
-- ==============================================================================

CREATE TABLE tenant_profiles (
    tenant_id UUID PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,

    -- 1. Company / Business Identity
    legal_name VARCHAR(255),
    trade_name VARCHAR(255),
    logo_url VARCHAR(500),
    website VARCHAR(255),
    incorporation_date DATE,
    company_type VARCHAR(100),

    -- 2. Registered Address
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    state_code VARCHAR(10),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'India',

    -- 3. Contact Information
    primary_email VARCHAR(255),
    primary_phone VARCHAR(50),
    secondary_phone VARCHAR(50),
    support_email VARCHAR(255),

    -- 4. Tax / GST Information
    gstin VARCHAR(15),
    pan VARCHAR(10),
    tan VARCHAR(20),
    is_reverse_charge_applicable BOOLEAN DEFAULT FALSE,
    composition_scheme BOOLEAN DEFAULT FALSE,
    lut_number VARCHAR(100),

    -- 5. Banking Information
    bank_name VARCHAR(150),
    account_number VARCHAR(50),
    ifsc_code VARCHAR(20),
    branch_name VARCHAR(150),
    account_type VARCHAR(50),
    upi_id VARCHAR(100),

    -- 6. Textile Business Specific Information
    textile_business_types VARCHAR(255),
    mill_capacity_details VARCHAR(500),
    standard_measurement_unit VARCHAR(50) DEFAULT 'METERS',
    loom_types VARCHAR(255),

    -- 7. Invoice / Business Document Configuration
    invoice_prefix VARCHAR(20) DEFAULT 'INV-',
    invoice_starting_sequence BIGINT DEFAULT 1,
    default_payment_terms_days INTEGER DEFAULT 30,
    terms_and_conditions TEXT,
    declaration_text TEXT,
    signature_image_url VARCHAR(500),

    -- 8. General ERP Settings
    fiscal_year_start_month INTEGER DEFAULT 4,
    default_currency VARCHAR(10) DEFAULT 'INR',
    time_zone VARCHAR(50) DEFAULT 'Asia/Kolkata',
    date_format VARCHAR(30) DEFAULT 'DD/MM/YYYY',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenant_profiles_gstin ON tenant_profiles(gstin);
CREATE INDEX idx_tenant_profiles_pan ON tenant_profiles(pan);
