-- =============================================================================
-- V8: Invoice Documents and Transactional Outbox Schema
-- =============================================================================

CREATE TABLE IF NOT EXISTS invoice_documents (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    invoice_id UUID NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    storage_provider VARCHAR(20) NOT NULL,
    storage_key VARCHAR(1000) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64),
    status VARCHAR(30) NOT NULL,
    failure_reason TEXT,
    generated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_documents_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT uk_invoice_documents_tenant_invoice_type UNIQUE (tenant_id, invoice_id, document_type)
);

CREATE INDEX IF NOT EXISTS idx_invoice_documents_tenant_invoice ON invoice_documents(tenant_id, invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoice_documents_tenant_status ON invoice_documents(tenant_id, status);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status ON outbox_events(status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_events_tenant_agg ON outbox_events(tenant_id, aggregate_id);
