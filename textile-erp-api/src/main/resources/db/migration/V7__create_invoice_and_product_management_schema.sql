-- ==============================================================================
-- Migration: V7__create_invoice_and_product_management_schema.sql
-- Description: Schema for Product Master, Agent Master, Invoices, Items,
--              Bales, Payments, and Customer Ledger Entries
-- ==============================================================================

-- 1. Product Master table
CREATE TABLE products (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_code VARCHAR(50) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    hsn_code VARCHAR(20),
    fabric_type VARCHAR(100),
    color VARCHAR(50),
    gsm INTEGER,
    unit VARCHAR(20) NOT NULL DEFAULT 'METER',
    default_rate_per_unit NUMERIC(12,2) DEFAULT 0.00,
    gst_rate NUMERIC(5,2) NOT NULL DEFAULT 5.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_products_tenant_code UNIQUE(tenant_id, product_code)
);

CREATE INDEX idx_products_tenant_id ON products(tenant_id);
CREATE INDEX idx_products_tenant_code ON products(tenant_id, product_code);
CREATE INDEX idx_products_tenant_name ON products(tenant_id, product_name);
CREATE INDEX idx_products_tenant_active ON products(tenant_id, active);

-- 2. Agent Master table
CREATE TABLE agents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    agent_name VARCHAR(255) NOT NULL,
    mobile VARCHAR(50),
    gstin VARCHAR(15),
    pan VARCHAR(10),
    address TEXT,
    default_commission_type VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',
    default_commission_value NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agents_tenant_id ON agents(tenant_id);
CREATE INDEX idx_agents_tenant_name ON agents(tenant_id, agent_name);

-- 3. Invoices table
CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_number VARCHAR(50) NOT NULL,
    financial_year VARCHAR(10) NOT NULL,
    invoice_date DATE NOT NULL,
    invoice_type VARCHAR(30) NOT NULL DEFAULT 'TAX_INVOICE',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

    customer_id UUID NOT NULL REFERENCES customers(id),
    shipping_address_id UUID REFERENCES shipping_parties(id),
    agent_id UUID REFERENCES agents(id),

    -- Immutable snapshots stored as JSON text
    seller_snapshot_json TEXT,
    billing_snapshot_json TEXT,
    shipping_snapshot_json TEXT,
    bank_snapshot_json TEXT,
    agent_name_snapshot VARCHAR(255),
    terms_and_conditions_snapshot TEXT,

    -- Financial amounts
    subtotal NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    cgst_rate NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    cgst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    sgst_rate NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    sgst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    igst_rate NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    igst_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    round_off_amount NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    grand_total NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    amount_in_words TEXT,

    -- Payment terms & tracking
    payment_mode VARCHAR(30),
    credit_days INTEGER DEFAULT 0,
    due_date DATE,
    payment_terms_note TEXT,
    total_paid_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    due_amount NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    payment_status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',

    -- Agent Commission
    commission_type VARCHAR(20),
    commission_value NUMERIC(10,2) DEFAULT 0.00,
    commission_amount NUMERIC(15,2) DEFAULT 0.00,
    agent_notes TEXT,

    -- Audit & lifecycle
    cancellation_reason TEXT,
    pdf_url VARCHAR(500),
    created_by UUID,
    issued_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_invoices_tenant_number UNIQUE(tenant_id, invoice_number)
);

CREATE INDEX idx_invoices_tenant_id ON invoices(tenant_id);
CREATE INDEX idx_invoices_tenant_number ON invoices(tenant_id, invoice_number);
CREATE INDEX idx_invoices_tenant_status ON invoices(tenant_id, status);
CREATE INDEX idx_invoices_tenant_date ON invoices(tenant_id, invoice_date);
CREATE INDEX idx_invoices_tenant_customer ON invoices(tenant_id, customer_id);
CREATE INDEX idx_invoices_tenant_payment_status ON invoices(tenant_id, payment_status);

-- 4. Invoice Items table
CREATE TABLE invoice_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id),

    description_snapshot TEXT,
    hsn_code_snapshot VARCHAR(20),
    unit VARCHAR(20) NOT NULL DEFAULT 'METER',

    meters NUMERIC(12,3) NOT NULL,
    folding_less_percent NUMERIC(6,3) NOT NULL DEFAULT 0.000,
    folding_less_meters NUMERIC(12,3) NOT NULL DEFAULT 0.000,
    total_meters NUMERIC(12,3) NOT NULL,
    rate_per_meter NUMERIC(12,2) NOT NULL,
    taxable_amount NUMERIC(15,2) NOT NULL,

    bale_details TEXT,
    total_bales INTEGER DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invoice_items_tenant_invoice ON invoice_items(tenant_id, invoice_id);

-- 5. Invoice Bales table
CREATE TABLE invoice_bales (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    invoice_item_id UUID REFERENCES invoice_items(id) ON DELETE CASCADE,
    bale_number VARCHAR(50),
    piece_count INTEGER,
    meters NUMERIC(12,3),
    net_weight NUMERIC(10,2),
    gross_weight NUMERIC(10,2),
    remarks VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invoice_bales_tenant_invoice ON invoice_bales(tenant_id, invoice_id);

-- 6. Invoice Payments table
CREATE TABLE invoice_payments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    payment_date DATE NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    payment_mode VARCHAR(30) NOT NULL,
    reference_number VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invoice_payments_tenant_invoice ON invoice_payments(tenant_id, invoice_id);

-- 7. Customer Ledger Entries table
CREATE TABLE customer_ledger_entries (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    invoice_id UUID REFERENCES invoices(id) ON DELETE SET NULL,
    entry_date DATE NOT NULL,
    entry_type VARCHAR(20) NOT NULL, -- DEBIT, CREDIT
    amount NUMERIC(15,2) NOT NULL,
    running_balance NUMERIC(15,2) NOT NULL DEFAULT 0.00,
    reference_type VARCHAR(50) NOT NULL, -- INVOICE, PAYMENT, INVOICE_CANCELLATION, OPENING_BALANCE
    reference_number VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ledger_tenant_customer ON customer_ledger_entries(tenant_id, customer_id);
CREATE INDEX idx_ledger_tenant_date ON customer_ledger_entries(tenant_id, entry_date);
