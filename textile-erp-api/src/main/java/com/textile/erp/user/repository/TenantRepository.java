package com.textile.erp.user.repository;

import com.textile.erp.user.entity.Tenant;
import com.textile.erp.user.entity.TenantStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    @Query("SELECT t FROM Tenant t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.slug) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Tenant> searchTenants(@Param("search") String search,
                               @Param("status") TenantStatus status,
                               Pageable pageable);
}

