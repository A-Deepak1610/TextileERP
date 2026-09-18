package com.textile.erp.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
public class InvoiceItemRequest {

    private UUID productId;
    private String description;
    private String hsnCode;

    @Builder.Default
    private String unit = "METER";

    @NotNull(message = "Meters are required")
    @DecimalMin(value = "0.001", message = "Meters must be greater than zero")
    private BigDecimal meters;

    @Builder.Default
    private BigDecimal foldingLessPercent = BigDecimal.ZERO;

    @NotNull(message = "Rate per meter is required")
    @DecimalMin(value = "0.01", message = "Rate must be greater than zero")
    private BigDecimal ratePerMeter;

    private String baleDetails;
    private Integer totalBales;
    private List<InvoiceBaleRequest> bales;
}
