package com.textile.erp.invoice.persistence;

import com.textile.erp.invoice.domain.InvoicePdfStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceDocumentRepository extends JpaRepository<InvoiceDocumentEntity, UUID> {

    Optional<InvoiceDocumentEntity> findByTenantIdAndInvoiceIdAndDocumentType(
            UUID tenantId, UUID invoiceId, String documentType);

    Optional<InvoiceDocumentEntity> findByTenantIdAndInvoiceId(UUID tenantId, UUID invoiceId);

    boolean existsByTenantIdAndInvoiceIdAndStatus(
            UUID tenantId, UUID invoiceId, InvoicePdfStatus status);
}
