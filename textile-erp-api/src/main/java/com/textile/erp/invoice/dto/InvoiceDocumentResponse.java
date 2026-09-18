package com.textile.erp.invoice.dto;

import com.textile.erp.invoice.domain.InvoicePdfStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDocumentResponse {
    private UUID id;
    private UUID tenantId;
    private UUID invoiceId;
    private String documentType;
    private String storageProvider;
    private String storageKey;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String checksumSha256;
    private InvoicePdfStatus status;
    private String failureReason;
    private Instant generatedAt;
    private Instant createdAt;
}
