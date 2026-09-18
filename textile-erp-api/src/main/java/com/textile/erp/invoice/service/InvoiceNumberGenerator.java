package com.textile.erp.invoice.service;

import com.textile.erp.invoice.repository.InvoiceRepository;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final InvoiceRepository invoiceRepository;

    public String generateNextInvoiceNumber(UUID tenantId, LocalDate invoiceDate) {
        String financialYear = calculateFinancialYear(invoiceDate);
        long sequence = invoiceRepository.countByTenantIdAndFinancialYear(tenantId, financialYear) + 1;

        String invoiceNumber = formatInvoiceNumber(financialYear, sequence);
        while (invoiceRepository.existsByTenantIdAndInvoiceNumber(tenantId, invoiceNumber)) {
            sequence++;
            invoiceNumber = formatInvoiceNumber(financialYear, sequence);
        }

        return invoiceNumber;
    }

    public String calculateFinancialYear(LocalDate date) {
        LocalDate target = (date != null) ? date : LocalDate.now();
        int year = target.getYear();
        int month = target.getMonthValue();

        if (month >= 4) {
            int nextYear = (year + 1) % 100;
            return String.format("%04d-%02d", year, nextYear);
        } else {
            int currentYearShort = year % 100;
            return String.format("%04d-%02d", year - 1, currentYearShort);
        }
    }

    private String formatInvoiceNumber(String financialYear, long sequence) {
        return String.format("SAL/%s/%04d", financialYear, sequence);
    }
}
