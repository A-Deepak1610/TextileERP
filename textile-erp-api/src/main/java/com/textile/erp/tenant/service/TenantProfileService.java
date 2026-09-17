package com.textile.erp.tenant.service;

import com.textile.erp.tenant.dto.TenantProfileResponse;
import com.textile.erp.tenant.dto.UpdateTenantProfileRequest;
import java.util.UUID;

public interface TenantProfileService {

    TenantProfileResponse getCurrentTenantProfile();

    TenantProfileResponse getTenantProfileById(UUID tenantId);

    TenantProfileResponse updateCurrentTenantProfile(UpdateTenantProfileRequest request);

    TenantProfileResponse updateTenantProfileById(UUID tenantId, UpdateTenantProfileRequest request);
}
