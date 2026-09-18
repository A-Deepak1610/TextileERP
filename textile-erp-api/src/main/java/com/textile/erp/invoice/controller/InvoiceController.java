package com.textile.erp.invoice.controller;

import com.textile.erp.invoice.dto.CancelInvoiceRequest;
import com.textile.erp.invoice.dto.CreateInvoiceRequest;
import com.textile.erp.invoice.dto.InvoiceResponse;
import com.textile.erp.invoice.dto.InvoiceSummaryResponse;
import com.textile.erp.invoice.dto.RecordPaymentRequest;
import com.textile.erp.invoice.dto.UpdateInvoiceRequest;
import com.textile.erp.invoice.entity.InvoiceStatus;
import com.textile.erp.invoice.entity.PaymentStatus;
import com.textile.erp.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoice Management", description = "Tax invoice creation, calculations, issuance, cancellation, and payments")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @Operation(summary = "Create invoice", description = "Create a new draft or issued tax invoice with full textile calculations, GST, and immutable snapshots.")
    @ApiResponse(responseCode = "201", description = "Invoice created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or calculation error")
    @ApiResponse(responseCode = "404", description = "Customer, product, or agent not found")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Search invoices", description = "Fetch paginated invoices with optional search, status, payment status, customer, and date range filters.")
    @ApiResponse(responseCode = "200", description = "Invoice list page returned")
    public ResponseEntity<Page<InvoiceSummaryResponse>> searchInvoices(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<InvoiceSummaryResponse> result = invoiceService.searchInvoices(query, status, paymentStatus, customerId, startDate, endDate, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID", description = "Fetch complete invoice details, line items, snapshots, and payments by ID.")
    @ApiResponse(responseCode = "200", description = "Invoice returned")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable UUID id) {
        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update draft invoice", description = "Update items, dates, and terms of an un-issued DRAFT invoice. Issued invoices cannot be modified.")
    @ApiResponse(responseCode = "200", description = "Draft invoice updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @ApiResponse(responseCode = "409", description = "Cannot edit issued or cancelled invoice")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.updateInvoice(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/issue")
    @Operation(summary = "Issue draft invoice", description = "Finalize and issue a draft invoice. Generates official sequence number (SAL/YYYY-YY/0001), renders PDF, locks invoice as immutable, and posts debit to customer ledger.")
    @ApiResponse(responseCode = "200", description = "Invoice issued successfully")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @ApiResponse(responseCode = "409", description = "Invoice is not in DRAFT status")
    public ResponseEntity<InvoiceResponse> issueInvoice(@PathVariable UUID id) {
        InvoiceResponse response = invoiceService.issueInvoice(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel issued invoice", description = "Cancel an issued invoice with a mandatory cancellation reason and post a credit reversal to the customer ledger.")
    @ApiResponse(responseCode = "200", description = "Invoice cancelled successfully")
    @ApiResponse(responseCode = "400", description = "Cancellation reason required")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @ApiResponse(responseCode = "409", description = "Cannot cancel draft or already cancelled invoice")
    public ResponseEntity<InvoiceResponse> cancelInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody CancelInvoiceRequest request) {
        InvoiceResponse response = invoiceService.cancelInvoice(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/payments")
    @Operation(summary = "Record invoice payment", description = "Record full or partial payment against an issued invoice, update payment status, and post credit to customer ledger.")
    @ApiResponse(responseCode = "200", description = "Payment recorded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payment amount or mode")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @ApiResponse(responseCode = "409", description = "Payments only permitted on ISSUED invoices")
    public ResponseEntity<InvoiceResponse> recordPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordPaymentRequest request) {
        InvoiceResponse response = invoiceService.recordPayment(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete draft invoice", description = "Delete a draft invoice. Issued invoices cannot be deleted (only cancelled).")
    @ApiResponse(responseCode = "204", description = "Draft invoice deleted")
    @ApiResponse(responseCode = "404", description = "Invoice not found")
    @ApiResponse(responseCode = "409", description = "Cannot delete issued or cancelled invoice")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.noContent().build();
    }
}
