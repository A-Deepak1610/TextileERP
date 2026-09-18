package com.textile.erp.product.dto;

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
public class ProductResponse {

    private UUID id;
    private UUID tenantId;
    private String productCode;
    private String productName;
    private String description;
    private String hsnCode;
    private String fabricType;
    private String color;
    private Integer gsm;
    private String unit;
    private BigDecimal defaultRatePerUnit;
    private BigDecimal gstRate;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
