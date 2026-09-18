package com.textile.erp.invoice.repository;

import com.textile.erp.invoice.entity.Invoice;
import com.textile.erp.invoice.entity.InvoiceStatus;
import com.textile.erp.invoice.entity.PaymentStatus;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<Invoice> findByTenantIdAndInvoiceNumber(UUID tenantId, String invoiceNumber);

    boolean existsByTenantIdAndInvoiceNumber(UUID tenantId, String invoiceNumber);

    @Query("""
        SELECT COUNT(i) FROM Invoice i
        WHERE i.tenantId = :tenantId
          AND i.financialYear = :financialYear
        """)
    long countByTenantIdAndFinancialYear(
        @Param("tenantId") UUID tenantId,
        @Param("financialYear") String financialYear
    );

    @Query("""
        SELECT i FROM Invoice i
        WHERE i.tenantId = :tenantId
          AND (:status IS NULL OR i.status = :status)
          AND (:paymentStatus IS NULL OR i.paymentStatus = :paymentStatus)
          AND (:customerId IS NULL OR i.customerId = :customerId)
          AND (:startDate IS NULL OR i.invoiceDate >= :startDate)
          AND (:endDate IS NULL OR i.invoiceDate <= :endDate)
          AND (
              :query IS NULL OR :query = '' OR
              LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR
              LOWER(i.agentNameSnapshot) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        """)
    Page<Invoice> searchInvoices(
        @Param("tenantId") UUID tenantId,
        @Param("query") String query,
        @Param("status") InvoiceStatus status,
        @Param("paymentStatus") PaymentStatus paymentStatus,
        @Param("customerId") UUID customerId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable
    );
}
