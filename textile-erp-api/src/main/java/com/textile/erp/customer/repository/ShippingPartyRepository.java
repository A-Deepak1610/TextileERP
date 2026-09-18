package com.textile.erp.customer.repository;

import com.textile.erp.customer.entity.ShippingParty;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShippingPartyRepository extends JpaRepository<ShippingParty, UUID> {

    List<ShippingParty> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    Optional<ShippingParty> findByTenantIdAndCustomerIdAndId(UUID tenantId, UUID customerId, UUID id);

    Optional<ShippingParty> findByTenantIdAndId(UUID tenantId, UUID id);

    void deleteByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
}
