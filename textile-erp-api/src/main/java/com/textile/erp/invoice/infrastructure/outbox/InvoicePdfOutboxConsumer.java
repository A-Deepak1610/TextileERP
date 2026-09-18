package com.textile.erp.invoice.infrastructure.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.textile.erp.invoice.application.InvoicePdfApplicationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePdfOutboxConsumer {

    public static final String EVENT_TYPE_INVOICE_PDF_REQUESTED = "INVOICE_PDF_GENERATION_REQUESTED";

    private final OutboxEventRepository outboxEventRepository;
    private final InvoicePdfApplicationService invoicePdfApplicationService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 3000)
    public void processPendingEvents() {
        List<OutboxEventEntity> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        if (pending.isEmpty()) {
            return;
        }

        for (OutboxEventEntity event : pending) {
            processSingleEvent(event);
        }
    }

    @Transactional
    public void processSingleEvent(OutboxEventEntity event) {
        if (!EVENT_TYPE_INVOICE_PDF_REQUESTED.equals(event.getEventType())) {
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(event.getPayload());
            UUID invoiceId = UUID.fromString(node.get("invoiceId").asText());
            UUID tenantId = UUID.fromString(node.get("tenantId").asText());

            log.info("Outbox consumer processing PDF request for invoice {} (tenant: {})", invoiceId, tenantId);
            invoicePdfApplicationService.generatePdf(invoiceId, tenantId, false);

            event.setStatus("PROCESSED");
            event.setProcessedAt(Instant.now());
            event.setErrorMessage(null);
            outboxEventRepository.saveAndFlush(event);
            log.info("Outbox event {} processed successfully", event.getId());
        } catch (Exception e) {
            log.error("Error processing outbox event {}: {}", event.getId(), e.getMessage(), e);
            event.setRetryCount(event.getRetryCount() + 1);
            event.setErrorMessage(e.getMessage());
            if (event.getRetryCount() >= 5) {
                event.setStatus("FAILED");
            }
            outboxEventRepository.saveAndFlush(event);
        }
    }
}
