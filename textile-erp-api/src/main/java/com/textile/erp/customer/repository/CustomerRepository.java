package com.textile.erp.customer.repository;

import com.textile.erp.customer.entity.Customer;
import com.textile.erp.customer.entity.CustomerStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByTenantIdAndId(UUID tenantId, UUID id);

    boolean existsByTenantIdAndCustomerCode(UUID tenantId, String customerCode);

    boolean existsByTenantIdAndCustomerCodeAndIdNot(UUID tenantId, String customerCode, UUID id);

    boolean existsByTenantIdAndGstin(UUID tenantId, String gstin);

    boolean existsByTenantIdAndGstinAndIdNot(UUID tenantId, String gstin, UUID id);

    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:search IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.gstin) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Customer> searchCustomers(@Param("tenantId") UUID tenantId,
                                 @Param("search") String search,
                                 @Param("status") CustomerStatus status,
                                 Pageable pageable);
}
