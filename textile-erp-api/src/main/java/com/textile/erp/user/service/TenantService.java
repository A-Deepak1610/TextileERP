package com.textile.erp.user.service;

import com.textile.erp.user.dto.TenantRequestDto;
import com.textile.erp.user.dto.TenantResponseDto;
import java.util.UUID;

public interface TenantService {

    TenantResponseDto createTenant(TenantRequestDto requestDto);

    TenantResponseDto getTenantById(UUID id);

    TenantResponseDto getTenantBySlug(String slug);

    boolean existsById(UUID id);
}
