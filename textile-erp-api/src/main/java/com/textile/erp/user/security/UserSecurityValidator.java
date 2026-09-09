package com.textile.erp.user.security;

import com.textile.erp.auth.security.CurrentUser;
import com.textile.erp.auth.security.SecurityUtils;
import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.User;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class UserSecurityValidator {

    public CurrentUser getAuthenticatedUser() {
        return SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Full authentication is required to access this resource"));
    }

    public void validateCanAccessUser(User targetUser) {
        CurrentUser currentUser = getAuthenticatedUser();

        if (currentUser.isSuperAdmin()) {
            return;
        }

        if (currentUser.hasRole(RoleName.TENANT_ADMIN)) {
            if (targetUser.isPlatformUser()) {
                throw new AccessDeniedException("Tenant administrators cannot view or modify platform-level users");
            }
            if (!Objects.equals(currentUser.getTenantId(), targetUser.getTenantId())) {
                throw new AccessDeniedException("Cross-tenant access forbidden: user does not belong to your tenant");
            }
            return;
        }

        // Standard employees can only access their own user record
        if (!Objects.equals(currentUser.getUserId(), targetUser.getId())) {
            throw new AccessDeniedException("Employees are not authorized to view or modify other users");
        }
    }

    public UUID resolveEffectiveTenantIdForCreation(RoleName requestedRole, UUID requestedTenantId) {
        CurrentUser currentUser = getAuthenticatedUser();

        if (currentUser.isSuperAdmin()) {
            if (requestedRole == RoleName.SUPER_ADMIN) {
                if (requestedTenantId != null) {
                    throw new IllegalArgumentException("SUPER_ADMIN is a platform user and must have tenantId = null");
                }
                return null;
            } else {
                if (requestedTenantId == null) {
                    throw new IllegalArgumentException("Tenant users must specify a valid tenantId");
                }
                return requestedTenantId;
            }
        }

        if (currentUser.hasRole(RoleName.TENANT_ADMIN)) {
            if (requestedRole == RoleName.SUPER_ADMIN) {
                throw new AccessDeniedException("Tenant administrators are forbidden from creating SUPER_ADMIN users");
            }
            // Tenant admin always provisions strictly within their own tenant
            return currentUser.getTenantId();
        }

        throw new AccessDeniedException("Employees are not authorized to provision new users");
    }

    public void validateCanModifyUser(User targetUser) {
        validateCanAccessUser(targetUser);

        CurrentUser currentUser = getAuthenticatedUser();
        if (currentUser.isSuperAdmin()) {
            return;
        }

        if (currentUser.hasRole(RoleName.TENANT_ADMIN)) {
            if (targetUser.isPlatformUser()) {
                throw new AccessDeniedException("Tenant administrators cannot modify platform users");
            }
            return;
        }

        throw new AccessDeniedException("Employees are not authorized to modify users");
    }

    public void validateCanManageRoles(User targetUser, RoleName roleToManage) {
        validateCanModifyUser(targetUser);

        CurrentUser currentUser = getAuthenticatedUser();
        if (currentUser.isSuperAdmin()) {
            return;
        }

        if (currentUser.hasRole(RoleName.TENANT_ADMIN)) {
            if (roleToManage == RoleName.SUPER_ADMIN) {
                throw new AccessDeniedException("Tenant administrators cannot assign or remove the SUPER_ADMIN role");
            }
            return;
        }

        throw new AccessDeniedException("Employees are not authorized to modify user roles");
    }
}
