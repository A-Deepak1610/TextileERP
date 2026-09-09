package com.textile.erp.auth.security;

import com.textile.erp.user.entity.RoleName;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class CurrentUser implements Serializable {

    private final UUID userId;
    private final UUID tenantId;
    private final String email;
    private final Set<RoleName> roles;

    public CurrentUser(UUID userId, UUID tenantId, String email, Set<RoleName> roles) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.email = email;
        this.roles = roles != null ? Collections.unmodifiableSet(roles) : Collections.emptySet();
    }

    public boolean isSuperAdmin() {
        return roles != null && roles.contains(RoleName.SUPER_ADMIN);
    }

    public boolean isPlatformUser() {
        return this.tenantId == null;
    }

    public boolean hasRole(RoleName roleName) {
        return roles != null && roles.contains(roleName);
    }
}
