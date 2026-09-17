package com.textile.erp.tenant.service;

import com.textile.erp.auth.security.CurrentUser;
import com.textile.erp.auth.security.SecurityUtils;
import com.textile.erp.tenant.dto.TenantProfileResponse;
import com.textile.erp.tenant.dto.UpdateTenantProfileRequest;
import com.textile.erp.tenant.entity.Address;
import com.textile.erp.tenant.entity.BankInfo;
import com.textile.erp.tenant.entity.CompanyIdentity;
import com.textile.erp.tenant.entity.ContactInfo;
import com.textile.erp.tenant.entity.ErpSettings;
import com.textile.erp.tenant.entity.InvoiceConfig;
import com.textile.erp.tenant.entity.TaxInfo;
import com.textile.erp.tenant.entity.TenantProfile;
import com.textile.erp.tenant.entity.TextileBusinessInfo;
import com.textile.erp.tenant.repository.TenantProfileRepository;
import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.Tenant;
import com.textile.erp.user.repository.TenantRepository;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantProfileServiceImpl implements TenantProfileService {

    private final TenantProfileRepository tenantProfileRepository;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional
    public TenantProfileResponse getCurrentTenantProfile() {
        CurrentUser currentUser = getAuthenticatedUser();
        UUID tenantId = currentUser.getTenantId();

        if (tenantId == null) {
            throw new IllegalArgumentException("Platform SUPER_ADMIN must specify a target tenant ID to view a profile");
        }

        return getOrCreateProfile(tenantId);
    }

    @Override
    @Transactional
    public TenantProfileResponse getTenantProfileById(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        validateCanAccessTenant(tenantId);
        return getOrCreateProfile(tenantId);
    }

    @Override
    @Transactional
    public TenantProfileResponse updateCurrentTenantProfile(UpdateTenantProfileRequest request) {
        CurrentUser currentUser = getAuthenticatedUser();
        UUID tenantId = currentUser.getTenantId();

        if (tenantId == null) {
            throw new IllegalArgumentException("Platform SUPER_ADMIN must specify a target tenant ID to update a profile");
        }

        validateCanModifyTenant(tenantId);
        return applyProfileUpdates(tenantId, request);
    }

    @Override
    @Transactional
    public TenantProfileResponse updateTenantProfileById(UUID tenantId, UpdateTenantProfileRequest request) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        validateCanModifyTenant(tenantId);
        return applyProfileUpdates(tenantId, request);
    }

    private TenantProfileResponse getOrCreateProfile(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found with ID: " + tenantId));

        TenantProfile profile = tenantProfileRepository.findById(tenantId)
                .orElseGet(() -> {
                    TenantProfile newProfile = TenantProfile.builder()
                            .tenantId(tenantId)
                            .companyIdentity(CompanyIdentity.builder()
                                    .legalName(tenant.getName())
                                    .tradeName(tenant.getName())
                                    .build())
                            .registeredAddress(new Address())
                            .contactInfo(new ContactInfo())
                            .taxInfo(new TaxInfo())
                            .bankInfo(new BankInfo())
                            .textileBusinessInfo(new TextileBusinessInfo())
                            .invoiceConfig(new InvoiceConfig())
                            .erpSettings(new ErpSettings())
                            .build();
                    return tenantProfileRepository.saveAndFlush(newProfile);
                });

        return mapToResponse(tenant, profile);
    }

    private TenantProfileResponse applyProfileUpdates(UUID tenantId, UpdateTenantProfileRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found with ID: " + tenantId));

        TenantProfile profile = tenantProfileRepository.findById(tenantId)
                .orElseGet(() -> TenantProfile.builder().tenantId(tenantId).build());

        if (request != null) {
            if (request.getCompanyIdentity() != null) {
                profile.setCompanyIdentity(request.getCompanyIdentity());
            }
            if (request.getRegisteredAddress() != null) {
                profile.setRegisteredAddress(request.getRegisteredAddress());
            }
            if (request.getContactInfo() != null) {
                profile.setContactInfo(request.getContactInfo());
            }
            if (request.getTaxInfo() != null) {
                profile.setTaxInfo(request.getTaxInfo());
            }
            if (request.getBankInfo() != null) {
                profile.setBankInfo(request.getBankInfo());
            }
            if (request.getTextileBusinessInfo() != null) {
                profile.setTextileBusinessInfo(request.getTextileBusinessInfo());
            }
            if (request.getInvoiceConfig() != null) {
                profile.setInvoiceConfig(request.getInvoiceConfig());
            }
            if (request.getErpSettings() != null) {
                profile.setErpSettings(request.getErpSettings());
            }
        }

        TenantProfile saved = tenantProfileRepository.saveAndFlush(profile);
        return mapToResponse(tenant, saved);
    }

    private TenantProfileResponse mapToResponse(Tenant tenant, TenantProfile profile) {
        return TenantProfileResponse.builder()
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .tenantSlug(tenant.getSlug())
                .companyIdentity(profile.getCompanyIdentity())
                .registeredAddress(profile.getRegisteredAddress())
                .contactInfo(profile.getContactInfo())
                .taxInfo(profile.getTaxInfo())
                .bankInfo(profile.getBankInfo())
                .textileBusinessInfo(profile.getTextileBusinessInfo())
                .invoiceConfig(profile.getInvoiceConfig())
                .erpSettings(profile.getErpSettings())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private CurrentUser getAuthenticatedUser() {
        return SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Full authentication is required to access tenant profile"));
    }

    private void validateCanAccessTenant(UUID tenantId) {
        CurrentUser currentUser = getAuthenticatedUser();
        if (currentUser.isSuperAdmin()) {
            return;
        }
        if (currentUser.getTenantId() != null && Objects.equals(currentUser.getTenantId(), tenantId)) {
            return;
        }
        throw new AccessDeniedException("Access forbidden: you do not have permission to view another tenant's profile");
    }

    private void validateCanModifyTenant(UUID tenantId) {
        CurrentUser currentUser = getAuthenticatedUser();
        if (currentUser.isSuperAdmin()) {
            return;
        }
        if (currentUser.hasRole(RoleName.TENANT_ADMIN) && Objects.equals(currentUser.getTenantId(), tenantId)) {
            return;
        }
        throw new AccessDeniedException("Access forbidden: only tenant administrator or SUPER_ADMIN can modify business configuration");
    }
}
