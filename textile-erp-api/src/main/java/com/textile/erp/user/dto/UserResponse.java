package com.textile.erp.user.dto;

import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.UserStatus;
import java.time.Instant;
import java.util.List;
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
public class UserResponse {

    private UUID id;
    private UUID tenantId;
    private String email;
    private String firstName;
    private String lastName;
    private UserStatus status;
    private List<RoleName> roles;
    private Instant createdAt;
    private Instant updatedAt;
}
