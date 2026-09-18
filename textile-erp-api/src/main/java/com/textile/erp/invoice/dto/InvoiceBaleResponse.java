package com.textile.erp.invoice.dto;

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
public class InvoiceBaleResponse {
    private UUID id;
    private String baleNumber;
    private Integer pieceCount;
    private BigDecimal meters;
    private BigDecimal netWeight;
    private BigDecimal grossWeight;
    private String remarks;
    private Instant createdAt;
}
