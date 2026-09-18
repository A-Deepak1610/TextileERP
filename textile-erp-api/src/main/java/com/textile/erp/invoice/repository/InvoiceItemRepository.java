package com.textile.erp.invoice.repository;

import com.textile.erp.invoice.entity.InvoiceItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {

    List<InvoiceItem> findByTenantIdAndInvoiceId(UUID tenantId, UUID invoiceId);
}
