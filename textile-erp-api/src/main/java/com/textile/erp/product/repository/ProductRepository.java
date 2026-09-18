package com.textile.erp.product.repository;

import com.textile.erp.product.entity.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<Product> findByTenantIdAndProductCode(UUID tenantId, String productCode);

    boolean existsByTenantIdAndProductCode(UUID tenantId, String productCode);

    boolean existsByTenantIdAndProductCodeAndIdNot(UUID tenantId, String productCode, UUID id);

    @Query("""
        SELECT p FROM Product p
        WHERE p.tenantId = :tenantId
          AND (:active IS NULL OR p.active = :active)
          AND (
              :query IS NULL OR :query = '' OR
              LOWER(p.productName) LIKE LOWER(CONCAT('%', :query, '%')) OR
              LOWER(p.productCode) LIKE LOWER(CONCAT('%', :query, '%')) OR
              LOWER(p.fabricType) LIKE LOWER(CONCAT('%', :query, '%')) OR
              LOWER(p.hsnCode) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        """)
    Page<Product> searchProducts(
        @Param("tenantId") UUID tenantId,
        @Param("query") String query,
        @Param("active") Boolean active,
        Pageable pageable
    );
}
