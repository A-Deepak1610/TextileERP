package com.textile.erp.invoice.infrastructure.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findTop50ByStatusOrderByCreatedAtAsc(String status);

    List<OutboxEventEntity> findByTenantIdAndAggregateId(UUID tenantId, UUID aggregateId);
}
