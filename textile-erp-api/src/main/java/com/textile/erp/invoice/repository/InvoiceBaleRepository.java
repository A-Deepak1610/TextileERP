package com.textile.erp.invoice.repository;

import com.textile.erp.invoice.entity.InvoiceBale;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceBaleRepository extends JpaRepository<InvoiceBale, UUID> {

    List<InvoiceBale> findByTenantIdAndInvoiceId(UUID tenantId, UUID invoiceId);
}
