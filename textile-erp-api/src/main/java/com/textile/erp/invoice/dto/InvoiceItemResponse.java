package com.textile.erp.invoice.dto;

import java.math.BigDecimal;
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
public class InvoiceItemResponse {
    private UUID id;
    private UUID productId;
    private String descriptionSnapshot;
    private String hsnCodeSnapshot;
    private String unit;
    private BigDecimal meters;
    private BigDecimal foldingLessPercent;
    private BigDecimal foldingLessMeters;
    private BigDecimal totalMeters;
    private BigDecimal ratePerMeter;
    private BigDecimal taxableAmount;
    private String baleDetails;
    private Integer totalBales;
    private List<InvoiceBaleResponse> bales;
    private Instant createdAt;
}
