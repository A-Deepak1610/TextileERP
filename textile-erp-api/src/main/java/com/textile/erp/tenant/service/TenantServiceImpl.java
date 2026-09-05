package com.textile.erp.tenant.service;

import com.textile.erp.tenant.dto.TenantRequestDto;
import com.textile.erp.tenant.dto.TenantResponseDto;
import com.textile.erp.tenant.entity.Tenant;
import com.textile.erp.tenant.entity.TenantStatus;
import com.textile.erp.tenant.repository.TenantRepository;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;

    @Override
    @Transactional
    public TenantResponseDto createTenant(TenantRequestDto requestDto) {
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

        Tenant saved = tenantRepository.save(tenant);
        return mapToResponseDto(saved);
    }

    @Override
    public TenantResponseDto getTenantById(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found with ID: " + id));
        return mapToResponseDto(tenant);
    }

    @Override
    public TenantResponseDto getTenantBySlug(String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug.trim().toLowerCase())
                .orElseThrow(() -> new NoSuchElementException("Tenant not found with slug: " + slug));
        return mapToResponseDto(tenant);
    }

    @Override
    public boolean existsById(UUID id) {
        return id != null && tenantRepository.existsById(id);
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
