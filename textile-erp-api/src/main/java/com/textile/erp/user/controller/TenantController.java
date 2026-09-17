package com.textile.erp.user.controller;

import com.textile.erp.user.dto.TenantRequestDto;
import com.textile.erp.user.dto.TenantResponseDto;
import com.textile.erp.user.dto.UpdateTenantRequest;
import com.textile.erp.user.dto.UpdateTenantStatusRequest;
import com.textile.erp.user.entity.TenantStatus;
import com.textile.erp.user.service.TenantService;
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
public class TenantController{

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<TenantResponseDto> createTenant(@RequestBody TenantRequestDto request) {
        TenantResponseDto response = tenantService.createTenantSecured(request);
        return ResponseEntity.created(URI.create("/api/tenants/" + response.getId())).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TenantResponseDto>> listTenants(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) TenantStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(tenantService.listTenants(search, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantResponseDto> getTenantById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(tenantService.getTenantByIdSecured(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<TenantResponseDto> getTenantBySlug(@PathVariable("slug") String slug) {
        return ResponseEntity.ok(tenantService.getTenantBySlugSecured(slug));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TenantResponseDto> updateTenant(
            @PathVariable("id") UUID id,
            @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(tenantService.updateTenant(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TenantResponseDto> updateTenantStatus(
            @PathVariable("id") UUID id,
            @RequestBody UpdateTenantStatusRequest request) {
        return ResponseEntity.ok(tenantService.updateTenantStatus(id, request));
    }
}
