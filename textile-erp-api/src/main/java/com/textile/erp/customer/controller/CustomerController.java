package com.textile.erp.customer.controller;

import com.textile.erp.customer.dto.CreateCustomerRequest;
import com.textile.erp.customer.dto.CreateShippingPartyRequest;
import com.textile.erp.customer.dto.CustomerResponse;
import com.textile.erp.customer.dto.CustomerSummaryResponse;
import com.textile.erp.customer.dto.ShippingPartyResponse;
import com.textile.erp.customer.dto.UpdateCustomerRequest;
import com.textile.erp.customer.dto.UpdateCustomerStatusRequest;
import com.textile.erp.customer.dto.UpdateShippingPartyRequest;
import com.textile.erp.customer.entity.CustomerStatus;
import com.textile.erp.customer.service.CustomerService;
import com.textile.erp.customer.service.ShippingPartyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Customer master and shipping destination party management")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService customerService;
    private final ShippingPartyService shippingPartyService;

    @PostMapping
    @Operation(summary = "Create customer", description = "Create a new customer master for the current tenant. Optionally supports same_as_billing to automatically generate shipping party.")
    @ApiResponse(responseCode = "201", description = "Customer created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or validation failed")
    @ApiResponse(responseCode = "409", description = "Customer code or GSTIN conflict")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get customers", description = "Fetch paginated customers for current tenant with optional search query (name, code, GSTIN, phone) and status filter.")
    @ApiResponse(responseCode = "200", description = "Customer page returned")
    public ResponseEntity<Page<CustomerSummaryResponse>> getCustomers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<CustomerSummaryResponse> result = customerService.listCustomers(query, status, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Fetch complete customer profile and shipping parties by ID for current tenant.")
    @ApiResponse(responseCode = "200", description = "Customer returned")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable UUID id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Update customer master attributes for current tenant.")
    @ApiResponse(responseCode = "200", description = "Customer updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or validation failed")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @ApiResponse(responseCode = "409", description = "Customer code or GSTIN conflict")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update customer status", description = "Update customer status (ACTIVE or INACTIVE).")
    @ApiResponse(responseCode = "200", description = "Customer status updated successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<CustomerResponse> updateCustomerStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerStatusRequest request) {
        CustomerResponse response = customerService.updateCustomerStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer", description = "Delete a customer and its associated shipping parties.")
    @ApiResponse(responseCode = "204", description = "Customer deleted successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }

    // Shipping parties nested routes

    @PostMapping("/{customerId}/shipping-parties")
    @Operation(summary = "Create shipping party", description = "Add a shipping destination party for a customer.")
    @ApiResponse(responseCode = "201", description = "Shipping party created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or validation failed")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<ShippingPartyResponse> createShippingParty(
            @PathVariable UUID customerId,
            @Valid @RequestBody CreateShippingPartyRequest request) {
        ShippingPartyResponse response = shippingPartyService.createShippingParty(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{customerId}/shipping-parties")
    @Operation(summary = "Get shipping parties", description = "List all shipping destinations/parties configured for a customer.")
    @ApiResponse(responseCode = "200", description = "Shipping parties list returned")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    public ResponseEntity<List<ShippingPartyResponse>> getShippingParties(@PathVariable UUID customerId) {
        List<ShippingPartyResponse> responses = shippingPartyService.getShippingParties(customerId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{customerId}/shipping-parties/{shippingPartyId}")
    @Operation(summary = "Get shipping party by ID", description = "Fetch a specific shipping destination party for a customer.")
    @ApiResponse(responseCode = "200", description = "Shipping party returned")
    @ApiResponse(responseCode = "404", description = "Customer or shipping party not found")
    public ResponseEntity<ShippingPartyResponse> getShippingPartyById(
            @PathVariable UUID customerId,
            @PathVariable UUID shippingPartyId) {
        ShippingPartyResponse response = shippingPartyService.getShippingPartyById(customerId, shippingPartyId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{customerId}/shipping-parties/{shippingPartyId}")
    @Operation(summary = "Update shipping party", description = "Update shipping destination details.")
    @ApiResponse(responseCode = "200", description = "Shipping party updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or validation failed")
    @ApiResponse(responseCode = "404", description = "Customer or shipping party not found")
    public ResponseEntity<ShippingPartyResponse> updateShippingParty(
            @PathVariable UUID customerId,
            @PathVariable UUID shippingPartyId,
            @Valid @RequestBody UpdateShippingPartyRequest request) {
        ShippingPartyResponse response = shippingPartyService.updateShippingParty(customerId, shippingPartyId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{customerId}/shipping-parties/{shippingPartyId}")
    @Operation(summary = "Delete shipping party", description = "Delete a shipping destination party from a customer.")
    @ApiResponse(responseCode = "204", description = "Shipping party deleted successfully")
    @ApiResponse(responseCode = "404", description = "Customer or shipping party not found")
    public ResponseEntity<Void> deleteShippingParty(
            @PathVariable UUID customerId,
            @PathVariable UUID shippingPartyId) {
        shippingPartyService.deleteShippingParty(customerId, shippingPartyId);
        return ResponseEntity.noContent().build();
    }
}
