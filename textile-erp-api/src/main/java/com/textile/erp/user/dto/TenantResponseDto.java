package com.textile.erp.user.dto;

import com.textile.erp.user.entity.TenantStatus;
import java.time.Instant;
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
public class TenantResponseDto {
    private UUID id;
    private String name;
    private String slug;
    private TenantStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID adminUserId;
    private String adminEmail;
}
