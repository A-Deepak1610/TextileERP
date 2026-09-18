package com.textile.erp.agent.repository;

import com.textile.erp.agent.entity.Agent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentRepository extends JpaRepository<Agent, UUID> {

    Optional<Agent> findByTenantIdAndId(UUID tenantId, UUID id);

    @Query("""
        SELECT a FROM Agent a
        WHERE a.tenantId = :tenantId
          AND (:active IS NULL OR a.active = :active)
          AND (
              :query IS NULL OR :query = '' OR
              LOWER(a.agentName) LIKE LOWER(CONCAT('%', :query, '%')) OR
              LOWER(a.mobile) LIKE LOWER(CONCAT('%', :query, '%')) OR
              LOWER(a.gstin) LIKE LOWER(CONCAT('%', :query, '%')) OR
              LOWER(a.pan) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        """)
    Page<Agent> searchAgents(
        @Param("tenantId") UUID tenantId,
        @Param("query") String query,
        @Param("active") Boolean active,
        Pageable pageable
    );
}
