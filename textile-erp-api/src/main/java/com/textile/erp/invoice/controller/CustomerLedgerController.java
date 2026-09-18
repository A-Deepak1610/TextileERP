package com.textile.erp.invoice.controller;

import com.textile.erp.invoice.dto.CustomerLedgerEntryResponse;
import com.textile.erp.invoice.service.CustomerLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/ledger")
@RequiredArgsConstructor
@Tag(name = "Customer Ledger", description = "Customer ledger statements and running balance tracking")
@SecurityRequirement(name = "bearerAuth")
public class CustomerLedgerController {

    private final CustomerLedgerService customerLedgerService;

    @GetMapping
    @Operation(summary = "Get customer ledger", description = "Fetch complete chronological ledger transactions (debits from invoices, credits from payments and reversals) with running balance.")
    @ApiResponse(responseCode = "200", description = "Ledger entries returned")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<List<CustomerLedgerEntryResponse>> getCustomerLedger(@PathVariable UUID customerId) {
        List<CustomerLedgerEntryResponse> entries = customerLedgerService.getCustomerLedger(customerId);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/balance")
    @Operation(summary = "Get customer balance", description = "Fetch the current outstanding debtor balance for a customer.")
    @ApiResponse(responseCode = "200", description = "Balance returned")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<Map<String, Object>> getCustomerBalance(@PathVariable UUID customerId) {
        BigDecimal balance = customerLedgerService.getCustomerOutstandingBalance(customerId);
        return ResponseEntity.ok(Map.of(
                "customerId", customerId,
                "outstandingBalance", balance
        ));
    }
}
