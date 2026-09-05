package com.textile.erp.user.dto;

import com.textile.erp.user.entity.RoleName;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequestDto {

    /**
     * Null for SUPER_ADMIN.
     * Mandatory for TENANT_ADMIN and EMPLOYEE.
     */
    private UUID tenantId;

    private String email;

    /**
     * Optional for OAuth accounts.
     */
    private String password;

    private String firstName;

    private String lastName;

    private RoleName role;
}
