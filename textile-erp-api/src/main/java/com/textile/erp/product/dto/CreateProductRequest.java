package com.textile.erp.product.dto;

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
public class CreateProductRequest {

    private String productCode;

    @NotBlank(message = "Product name is required")
    private String productName;

    private String description;
    private String hsnCode;
    private String fabricType;
    private String color;
    private Integer gsm;

    @Builder.Default
    private String unit = "METER";

    @DecimalMin(value = "0.0", message = "Default rate per unit cannot be negative")
    private BigDecimal defaultRatePerUnit;

    @DecimalMin(value = "0.0", message = "GST rate cannot be negative")
    private BigDecimal gstRate;

    @Builder.Default
    private Boolean active = true;
}
