package com.textile.erp.agent.dto;

import com.textile.erp.agent.entity.CommissionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
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
public class CreateAgentRequest {

    @NotBlank(message = "Agent name is required")
    private String agentName;

    private String mobile;
    private String gstin;
    private String pan;
    private String address;

    @Builder.Default
    private CommissionType defaultCommissionType = CommissionType.PERCENTAGE;

    @DecimalMin(value = "0.0", message = "Commission value cannot be negative")
    @Builder.Default
    private BigDecimal defaultCommissionValue = BigDecimal.ZERO;

    @Builder.Default
    private Boolean active = true;
}
