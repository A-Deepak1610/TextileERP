package com.textile.erp.invoice.dto;

import com.textile.erp.invoice.entity.LedgerEntryType;
import com.textile.erp.invoice.entity.LedgerReferenceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
public class CustomerLedgerEntryResponse {

    private UUID id;
    private UUID tenantId;
    private UUID customerId;
    private UUID invoiceId;
    private LocalDate entryDate;
    private LedgerEntryType entryType;
    private BigDecimal amount;
    private BigDecimal runningBalance;
    private LedgerReferenceType referenceType;
    private String referenceNumber;
    private String notes;
    private Instant createdAt;
}
