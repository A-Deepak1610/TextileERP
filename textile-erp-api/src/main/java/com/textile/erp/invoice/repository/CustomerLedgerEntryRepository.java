package com.textile.erp.invoice.repository;

import com.textile.erp.invoice.entity.CustomerLedgerEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerLedgerEntryRepository extends JpaRepository<CustomerLedgerEntry, UUID> {

    List<CustomerLedgerEntry> findByTenantIdAndCustomerIdOrderByEntryDateAscCreatedAtAsc(UUID tenantId, UUID customerId);

    Optional<CustomerLedgerEntry> findTopByTenantIdAndCustomerIdOrderByEntryDateDescCreatedAtDesc(UUID tenantId, UUID customerId);
}
