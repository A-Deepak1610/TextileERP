package com.textile.erp.user.controller;

import com.textile.erp.user.dto.TenantRequestDto;
import com.textile.erp.user.dto.TenantResponseDto;
import com.textile.erp.user.dto.UpdateTenantRequest;
import com.textile.erp.user.dto.UpdateTenantStatusRequest;
import com.textile.erp.user.entity.TenantStatus;
import com.textile.erp.user.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "Endpoints for creating, listing, retrieving, and updating tenants and organization profiles")
@SecurityRequirement(name = "bearerAuth")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @Operation(
            summary = "Create Tenant Organization",
            description = "Creates a new multi-tenant organization. The caller (SUPER_ADMIN) can also optionally provide admin credentials (email & password) to automatically provision the initial TENANT_ADMIN user for immediate login."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tenant created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error: blank name/slug or duplicate slug"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN role")
    })
    public ResponseEntity<TenantResponseDto> createTenant(@RequestBody TenantRequestDto request) {
        TenantResponseDto response = tenantService.createTenantSecured(request);
        return ResponseEntity.created(URI.create("/api/tenants/" + response.getId())).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List Tenants (Paginated & Filterable)",
            description = "Retrieves a paginated list of all tenant organizations with optional text search across name/slug and optional lifecycle status filtering. Restricted to SUPER_ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Paginated list of tenants returned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN role")
    })
    public ResponseEntity<Page<TenantResponseDto>> listTenants(
            @Parameter(description = "Search term matching tenant name or slug")
            @RequestParam(name = "search", required = false) String search,
            @Parameter(description = "Filter by tenant lifecycle status (ACTIVE, SUSPENDED, INACTIVE)")
            @RequestParam(name = "status", required = false) TenantStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(tenantService.listTenants(search, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get Tenant by ID",
            description = "Retrieves tenant profile details by UUID. Accessible by SUPER_ADMIN or any user assigned to this specific tenant."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cross-tenant access denied"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<TenantResponseDto> getTenantById(
            @Parameter(description = "Unique UUID identifier of the tenant")
            @PathVariable("id") UUID id) {
        return ResponseEntity.ok(tenantService.getTenantByIdSecured(id));
    }

    @GetMapping("/slug/{slug}")
    @Operation(
            summary = "Get Tenant by Slug",
            description = "Retrieves tenant profile details by its unique URL slug. Accessible by SUPER_ADMIN or users assigned to this specific tenant."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cross-tenant access denied"),
            @ApiResponse(responseCode = "404", description = "Tenant with specified slug not found")
    })
    public ResponseEntity<TenantResponseDto> getTenantBySlug(
            @Parameter(description = "Unique alphanumeric slug of the tenant")
            @PathVariable("slug") String slug) {
        return ResponseEntity.ok(tenantService.getTenantBySlugSecured(slug));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update Tenant Details",
            description = "Updates tenant profile properties such as name and slug. Accessible by SUPER_ADMIN or the TENANT_ADMIN of this tenant."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error: duplicate slug or invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only SUPER_ADMIN or tenant administrator can modify"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<TenantResponseDto> updateTenant(
            @Parameter(description = "Unique UUID identifier of the tenant")
            @PathVariable("id") UUID id,
            @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update Tenant Lifecycle Status",
            description = "Changes the lifecycle status of a tenant (ACTIVE, SUSPENDED, INACTIVE). Restricted exclusively to SUPER_ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error: invalid status"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - requires SUPER_ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<TenantResponseDto> updateTenantStatus(
            @Parameter(description = "Unique UUID identifier of the tenant")
            @PathVariable("id") UUID id,
            @RequestBody UpdateTenantStatusRequest request) {
        return ResponseEntity.ok(tenantService.updateTenantStatus(id, request));
    }
}
