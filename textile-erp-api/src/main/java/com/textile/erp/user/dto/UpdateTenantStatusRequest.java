package com.textile.erp.user.dto;

import com.textile.erp.user.entity.TenantStatus;
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
public class UpdateTenantStatusRequest {
    private TenantStatus status;
}
