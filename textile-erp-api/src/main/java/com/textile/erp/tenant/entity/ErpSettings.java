package com.textile.erp.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErpSettings {

    @Column(name = "fiscal_year_start_month")
    @Builder.Default
    private Integer fiscalYearStartMonth = 4;

    @Column(name = "default_currency", length = 10)
    @Builder.Default
    private String defaultCurrency = "INR";

    @Column(name = "time_zone", length = 50)
    @Builder.Default
    private String timeZone = "Asia/Kolkata";

    @Column(name = "date_format", length = 30)
    @Builder.Default
    private String dateFormat = "DD/MM/YYYY";
}
