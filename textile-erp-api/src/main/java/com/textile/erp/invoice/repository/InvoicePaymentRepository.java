package com.textile.erp.invoice.repository;

import com.textile.erp.invoice.entity.InvoicePayment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, UUID> {

    List<InvoicePayment> findByTenantIdAndInvoiceIdOrderByPaymentDateAscCreatedAtAsc(UUID tenantId, UUID invoiceId);
}
