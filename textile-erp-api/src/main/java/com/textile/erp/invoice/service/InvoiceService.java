package com.textile.erp.invoice.service;

import com.textile.erp.invoice.dto.CancelInvoiceRequest;
import com.textile.erp.invoice.dto.CreateInvoiceRequest;
import com.textile.erp.invoice.dto.InvoiceResponse;
import com.textile.erp.invoice.dto.InvoiceSummaryResponse;
import com.textile.erp.invoice.dto.RecordPaymentRequest;
import com.textile.erp.invoice.dto.UpdateInvoiceRequest;
import com.textile.erp.invoice.entity.InvoiceStatus;
import com.textile.erp.invoice.entity.PaymentStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvoiceService {

    InvoiceResponse createInvoice(CreateInvoiceRequest request);

    InvoiceResponse getInvoiceById(UUID id);

    Page<InvoiceSummaryResponse> searchInvoices(
            String query,
            InvoiceStatus status,
            PaymentStatus paymentStatus,
            UUID customerId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    InvoiceResponse updateInvoice(UUID id, UpdateInvoiceRequest request);

    InvoiceResponse issueInvoice(UUID id);

    InvoiceResponse cancelInvoice(UUID id, CancelInvoiceRequest request);

    InvoiceResponse recordPayment(UUID id, RecordPaymentRequest request);

    void deleteInvoice(UUID id);
}
