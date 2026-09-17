package com.textile.erp.tenant.repository;

import com.textile.erp.tenant.entity.TenantProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantProfileRepository extends JpaRepository<TenantProfile, UUID> {

    @Query("SELECT tp FROM TenantProfile tp WHERE LOWER(tp.taxInfo.gstin) = LOWER(:gstin)")
    Optional<TenantProfile> findByGstin(@Param("gstin") String gstin);

    @Query("SELECT tp FROM TenantProfile tp WHERE LOWER(tp.taxInfo.pan) = LOWER(:pan)")
    Optional<TenantProfile> findByPan(@Param("pan") String pan);
}
