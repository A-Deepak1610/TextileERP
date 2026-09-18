package com.textile.erp.invoice.service;

import com.textile.erp.auth.security.CurrentUser;
import com.textile.erp.auth.security.SecurityUtils;
import com.textile.erp.customer.repository.CustomerRepository;
import com.textile.erp.invoice.dto.CustomerLedgerEntryResponse;
import com.textile.erp.invoice.entity.CustomerLedgerEntry;
import com.textile.erp.invoice.entity.LedgerEntryType;
import com.textile.erp.invoice.entity.LedgerReferenceType;
import com.textile.erp.invoice.repository.CustomerLedgerEntryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerLedgerServiceImpl implements CustomerLedgerService {

    private final CustomerLedgerEntryRepository ledgerRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public CustomerLedgerEntry recordDebit(
            UUID tenantId,
            UUID customerId,
            UUID invoiceId,
            LocalDate date,
            BigDecimal amount,
            String refNumber,
            String notes) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be greater than zero");
        }

        BigDecimal lastBalance = getLastRunningBalance(tenantId, customerId);
        BigDecimal newBalance = lastBalance.add(amount).setScale(2, RoundingMode.HALF_UP);

        CustomerLedgerEntry entry = CustomerLedgerEntry.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .invoiceId(invoiceId)
                .entryDate(date != null ? date : LocalDate.now())
                .entryType(LedgerEntryType.DEBIT)
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .runningBalance(newBalance)
                .referenceType(LedgerReferenceType.INVOICE)
                .referenceNumber(refNumber)
                .notes(notes)
                .build();

        CustomerLedgerEntry saved = ledgerRepository.saveAndFlush(entry);
        log.info("Recorded DEBIT entry {} for customer {}, amount {}, running balance {}",
                saved.getId(), customerId, amount, newBalance);
        return saved;
    }

    @Override
    @Transactional
    public CustomerLedgerEntry recordCredit(
            UUID tenantId,
            UUID customerId,
            UUID invoiceId,
            LocalDate date,
            BigDecimal amount,
            LedgerReferenceType refType,
            String refNumber,
            String notes) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero");
        }

        BigDecimal lastBalance = getLastRunningBalance(tenantId, customerId);
        BigDecimal newBalance = lastBalance.subtract(amount).setScale(2, RoundingMode.HALF_UP);

        CustomerLedgerEntry entry = CustomerLedgerEntry.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .invoiceId(invoiceId)
                .entryDate(date != null ? date : LocalDate.now())
                .entryType(LedgerEntryType.CREDIT)
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .runningBalance(newBalance)
                .referenceType(refType != null ? refType : LedgerReferenceType.PAYMENT)
                .referenceNumber(refNumber)
                .notes(notes)
                .build();

        CustomerLedgerEntry saved = ledgerRepository.saveAndFlush(entry);
        log.info("Recorded CREDIT entry {} for customer {}, amount {}, running balance {}",
                saved.getId(), customerId, amount, newBalance);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerLedgerEntryResponse> getCustomerLedger(UUID customerId) {
        UUID tenantId = resolveCurrentTenantId();
        validateCustomerExists(tenantId, customerId);

        return ledgerRepository.findByTenantIdAndCustomerIdOrderByEntryDateAscCreatedAtAsc(tenantId, customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCustomerOutstandingBalance(UUID customerId) {
        UUID tenantId = resolveCurrentTenantId();
        validateCustomerExists(tenantId, customerId);
        return getLastRunningBalance(tenantId, customerId);
    }

    private BigDecimal getLastRunningBalance(UUID tenantId, UUID customerId) {
        return ledgerRepository.findTopByTenantIdAndCustomerIdOrderByEntryDateDescCreatedAtDesc(tenantId, customerId)
                .map(CustomerLedgerEntry::getRunningBalance)
                .orElse(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }

    private void validateCustomerExists(UUID tenantId, UUID customerId) {
        customerRepository.findByTenantIdAndId(tenantId, customerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found with ID: " + customerId));
    }

    private UUID resolveCurrentTenantId() {
        CurrentUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
        if (currentUser.getTenantId() != null) {
            return currentUser.getTenantId();
        }
        throw new IllegalArgumentException("Action requires an active tenant context");
    }

    private CustomerLedgerEntryResponse mapToResponse(CustomerLedgerEntry entry) {
        return CustomerLedgerEntryResponse.builder()
                .id(entry.getId())
                .tenantId(entry.getTenantId())
                .customerId(entry.getCustomerId())
                .invoiceId(entry.getInvoiceId())
                .entryDate(entry.getEntryDate())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .runningBalance(entry.getRunningBalance())
                .referenceType(entry.getReferenceType())
                .referenceNumber(entry.getReferenceNumber())
                .notes(entry.getNotes())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
