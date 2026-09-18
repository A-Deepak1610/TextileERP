package com.textile.erp.invoice.service;

import com.textile.erp.invoice.dto.CustomerLedgerEntryResponse;
import com.textile.erp.invoice.entity.CustomerLedgerEntry;
import com.textile.erp.invoice.entity.LedgerReferenceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CustomerLedgerService {

    CustomerLedgerEntry recordDebit(
            UUID tenantId,
            UUID customerId,
            UUID invoiceId,
            LocalDate date,
            BigDecimal amount,
            String refNumber,
            String notes
    );

    CustomerLedgerEntry recordCredit(
            UUID tenantId,
            UUID customerId,
            UUID invoiceId,
            LocalDate date,
            BigDecimal amount,
            LedgerReferenceType refType,
            String refNumber,
            String notes
    );

    List<CustomerLedgerEntryResponse> getCustomerLedger(UUID customerId);

    BigDecimal getCustomerOutstandingBalance(UUID customerId);
}
