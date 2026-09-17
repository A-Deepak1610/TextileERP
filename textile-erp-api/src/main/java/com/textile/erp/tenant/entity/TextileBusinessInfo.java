package com.textile.erp.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class TextileBusinessInfo {

    @Column(name = "textile_business_types")
    private String businessTypes;

    @Column(name = "mill_capacity_details", length = 500)
    private String millCapacityDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "standard_measurement_unit", length = 50)
    @Builder.Default
    private MeasurementUnit standardMeasurementUnit = MeasurementUnit.METERS;

    @Column(name = "loom_types")
    private String loomTypes;
}
