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
public class TaxInfo {

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "pan", length = 10)
    private String pan;

    @Column(name = "tan", length = 20)
    private String tan;

    @Column(name = "is_reverse_charge_applicable")
    @Builder.Default
    private Boolean reverseChargeApplicable = false;

    @Column(name = "composition_scheme")
    @Builder.Default
    private Boolean compositionScheme = false;

    @Column(name = "lut_number", length = 100)
    private String lutNumber;
}
