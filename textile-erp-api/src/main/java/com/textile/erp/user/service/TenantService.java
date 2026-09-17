package com.textile.erp.user.service;

import com.textile.erp.user.dto.TenantRequestDto;
import com.textile.erp.user.dto.TenantResponseDto;
import com.textile.erp.user.dto.UpdateTenantRequest;
import com.textile.erp.user.dto.UpdateTenantStatusRequest;
import com.textile.erp.user.entity.TenantStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TenantService {

    TenantResponseDto createTenant(TenantRequestDto requestDto);

    TenantResponseDto createTenantSecured(TenantRequestDto requestDto);

    TenantResponseDto getTenantById(UUID id);

    TenantResponseDto getTenantByIdSecured(UUID id);

    TenantResponseDto getTenantBySlug(String slug);

    TenantResponseDto getTenantBySlugSecured(String slug);

    boolean existsById(UUID id);

    Page<TenantResponseDto> listTenants(String search, TenantStatus status, Pageable pageable);

    TenantResponseDto updateTenant(UUID id, UpdateTenantRequest request);

    TenantResponseDto updateTenantStatus(UUID id, UpdateTenantStatusRequest request);
}

