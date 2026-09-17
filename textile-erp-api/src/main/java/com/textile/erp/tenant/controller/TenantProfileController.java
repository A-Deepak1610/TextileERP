package com.textile.erp.tenant.controller;

import com.textile.erp.tenant.dto.TenantProfileResponse;
import com.textile.erp.tenant.dto.UpdateTenantProfileRequest;
import com.textile.erp.tenant.service.TenantProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenant/profile")
@RequiredArgsConstructor
@Tag(name = "Tenant Profile & Business Config", description = "Endpoints for managing company identity, registered address, tax/GST, banking, textile operations, invoice template configs, and ERP settings")
@SecurityRequirement(name = "bearerAuth")
public class TenantProfileController {

    private final TenantProfileService tenantProfileService;

    @GetMapping
    @Operation(
            summary = "Get Current Tenant Profile & Business Configuration",
            description = "Retrieves the full business configuration and profile for the authenticated user's tenant organization. Automatically resolves tenant context from JWT."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant profile and business configuration returned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<TenantProfileResponse> getCurrentTenantProfile() {
        return ResponseEntity.ok(tenantProfileService.getCurrentTenantProfile());
    }

    @PutMapping
    @Operation(
            summary = "Update Current Tenant Profile & Business Configuration",
            description = "Updates the business configuration, address, tax/GST, banking, textile business details, invoice settings, and ERP preferences for the caller's tenant. Restricted to TENANT_ADMIN or SUPER_ADMIN."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant profile and business configuration updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - standard employees cannot update business configuration"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<TenantProfileResponse> updateCurrentTenantProfile(
            @RequestBody UpdateTenantProfileRequest request) {
        return ResponseEntity.ok(tenantProfileService.updateCurrentTenantProfile(request));
    }

    @GetMapping("/{tenantId}")
    @Operation(
            summary = "Get Tenant Profile by Tenant ID",
            description = "Retrieves the full business configuration for a specified tenant ID. Accessible by SUPER_ADMIN or any user assigned to this specific tenant."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant profile returned successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cross-tenant access denied"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<TenantProfileResponse> getTenantProfileById(
            @Parameter(description = "Unique UUID of the target tenant")
            @PathVariable("tenantId") UUID tenantId) {
        return ResponseEntity.ok(tenantProfileService.getTenantProfileById(tenantId));
    }

    @PutMapping("/{tenantId}")
    @Operation(
            summary = "Update Tenant Profile by Tenant ID",
            description = "Updates the business configuration for a specified tenant ID. Accessible by SUPER_ADMIN or the TENANT_ADMIN of that specific tenant."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tenant profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden - cross-tenant modification denied"),
            @ApiResponse(responseCode = "404", description = "Tenant not found")
    })
    public ResponseEntity<TenantProfileResponse> updateTenantProfileById(
            @Parameter(description = "Unique UUID of the target tenant")
            @PathVariable("tenantId") UUID tenantId,
            @RequestBody UpdateTenantProfileRequest request) {
        return ResponseEntity.ok(tenantProfileService.updateTenantProfileById(tenantId, request));
    }
}
