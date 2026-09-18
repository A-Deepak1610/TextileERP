package com.textile.erp.invoice.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OutboxEventEntity publishEvent(
            UUID tenantId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            Object payload) {

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize outbox event payload", e);
        }

        OutboxEventEntity event = OutboxEventEntity.builder()
                .tenantId(tenantId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payloadJson)
                .status("PENDING")
                .retryCount(0)
                .build();

        OutboxEventEntity saved = outboxEventRepository.saveAndFlush(event);
        log.info("Saved outbox event {} for {} ID: {} (tenant: {})", eventType, aggregateType, aggregateId, tenantId);
        return saved;
    }
}
