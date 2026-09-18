package com.textile.erp.invoice.dto;

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
public class InvoiceBaleRequest {
    private String baleNumber;
    private Integer pieceCount;
    private BigDecimal meters;
    private BigDecimal netWeight;
    private BigDecimal grossWeight;
    private String remarks;
}
