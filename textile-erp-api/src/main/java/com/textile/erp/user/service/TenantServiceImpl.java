package com.textile.erp.user.service;

import com.textile.erp.user.dto.TenantRequestDto;
import com.textile.erp.user.dto.TenantResponseDto;
import com.textile.erp.user.dto.UpdateTenantRequest;
import com.textile.erp.user.dto.UpdateTenantStatusRequest;
import com.textile.erp.user.entity.Tenant;
import com.textile.erp.user.entity.TenantStatus;
import com.textile.erp.user.repository.TenantRepository;
import com.textile.erp.user.security.UserSecurityValidator;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final UserSecurityValidator userSecurityValidator;

    @Override
    @Transactional
    public TenantResponseDto createTenant(TenantRequestDto requestDto) {
        if (requestDto == null) {
            throw new IllegalArgumentException("TenantRequestDto cannot be null");
        }
        if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant name cannot be blank");
        }
        if (requestDto.getSlug() == null || requestDto.getSlug().trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant slug cannot be blank");
        }

        String normalizedSlug = requestDto.getSlug().trim().toLowerCase();
        if (tenantRepository.existsBySlug(normalizedSlug)) {
            throw new IllegalArgumentException("Tenant with slug '" + normalizedSlug + "' already exists");
        }

        Tenant tenant = Tenant.builder()
                .name(requestDto.getName().trim())
                .slug(normalizedSlug)
                .status(TenantStatus.ACTIVE)
                .build();

        Tenant saved = tenantRepository.saveAndFlush(tenant);
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public TenantResponseDto createTenantSecured(TenantRequestDto requestDto) {
        userSecurityValidator.validateCanManageTenants();
        return createTenant(requestDto);
    }

    @Override
    public TenantResponseDto getTenantById(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found with ID: " + id));
        return mapToResponseDto(tenant);
    }

    @Override
    public TenantResponseDto getTenantByIdSecured(UUID id) {
        userSecurityValidator.validateCanAccessTenant(id);
        return getTenantById(id);
    }

    @Override
    public TenantResponseDto getTenantBySlug(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant slug cannot be blank");
        }
        Tenant tenant = tenantRepository.findBySlug(slug.trim().toLowerCase())
                .orElseThrow(() -> new NoSuchElementException("Tenant not found with slug: " + slug));
        return mapToResponseDto(tenant);
    }

    @Override
    public TenantResponseDto getTenantBySlugSecured(String slug) {
        if (slug == null || slug.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant slug cannot be blank");
        }
        Tenant tenant = tenantRepository.findBySlug(slug.trim().toLowerCase())
                .orElseThrow(() -> new NoSuchElementException("Tenant not found with slug: " + slug));
        userSecurityValidator.validateCanAccessTenant(tenant.getId());
        return mapToResponseDto(tenant);
    }

    @Override
    public boolean existsById(UUID id) {
        return id != null && tenantRepository.existsById(id);
    }

    @Override
    public Page<TenantResponseDto> listTenants(String search, TenantStatus status, Pageable pageable) {
        userSecurityValidator.validateCanManageTenants();
        String term = (search != null && !search.isBlank()) ? search.trim() : null;
        return tenantRepository.searchTenants(term, status, pageable).map(this::mapToResponseDto);
    }

    @Override
    @Transactional
    public TenantResponseDto updateTenant(UUID id, UpdateTenantRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("UpdateTenantRequest cannot be null");
        }
        userSecurityValidator.validateCanModifyTenant(id);

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found with ID: " + id));

        if (request.getName() != null) {
            if (request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Tenant name cannot be blank");
            }
            tenant.setName(request.getName().trim());
        }

        if (request.getSlug() != null) {
            String newSlug = request.getSlug().trim().toLowerCase();
            if (newSlug.isEmpty()) {
                throw new IllegalArgumentException("Tenant slug cannot be blank");
            }
            if (tenantRepository.existsBySlugAndIdNot(newSlug, id)) {
                throw new IllegalArgumentException("Tenant with slug '" + newSlug + "' already exists");
            }
            tenant.setSlug(newSlug);
        }

        Tenant updated = tenantRepository.saveAndFlush(tenant);
        return mapToResponseDto(updated);
    }

    @Override
    @Transactional
    public TenantResponseDto updateTenantStatus(UUID id, UpdateTenantStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("Tenant status must be specified");
        }
        userSecurityValidator.validateCanManageTenants();

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found with ID: " + id));

        tenant.setStatus(request.getStatus());
        Tenant updated = tenantRepository.saveAndFlush(tenant);
        return mapToResponseDto(updated);
    }

    private TenantResponseDto mapToResponseDto(Tenant tenant) {
        return TenantResponseDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .status(tenant.getStatus())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}
