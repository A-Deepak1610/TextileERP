package com.textile.erp.tenant.service;

import com.textile.erp.tenant.dto.TenantRequestDto;
import com.textile.erp.tenant.dto.TenantResponseDto;
import java.util.UUID;

public interface TenantService {

    TenantResponseDto createTenant(TenantRequestDto requestDto);

    TenantResponseDto getTenantById(UUID id);

    TenantResponseDto getTenantBySlug(String slug);

    boolean existsById(UUID id);
}
