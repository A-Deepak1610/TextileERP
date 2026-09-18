package com.textile.erp.invoice.infrastructure.jasper;

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
public class InvoiceItemPdfData {
    private Integer serialNumber;
    private String description;
    private String hsnCode;
    private String meters;
    private String foldingLessPercent;
    private String foldingLessMeters;
    private String totalMeters;
    private String rate;
    private String taxableAmount;
    private String baleDetails;
}
