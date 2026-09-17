package com.textile.erp.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;
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
public class CompanyIdentity {

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "website")
    private String website;

    @Column(name = "incorporation_date")
    private LocalDate incorporationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", length = 100)
    private CompanyType companyType;
}
