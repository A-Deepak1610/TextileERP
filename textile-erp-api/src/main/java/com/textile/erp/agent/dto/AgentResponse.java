package com.textile.erp.agent.dto;

import com.textile.erp.agent.entity.CommissionType;
import java.math.BigDecimal;
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
public class AgentResponse {

    private UUID id;
    private UUID tenantId;
    private String agentName;
    private String mobile;
    private String gstin;
    private String pan;
    private String address;
    private CommissionType defaultCommissionType;
    private BigDecimal defaultCommissionValue;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
