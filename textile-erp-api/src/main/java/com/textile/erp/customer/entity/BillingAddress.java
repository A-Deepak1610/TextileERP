package com.textile.erp.customer.entity;

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
public class BillingAddress {

    @Column(name = "billing_address_line1")
    private String addressLine1;

    @Column(name = "billing_address_line2")
    private String addressLine2;

    @Column(name = "billing_city", length = 100)
    private String city;

    @Column(name = "billing_district", length = 100)
    private String district;

    @Column(name = "billing_state", length = 100)
    private String state;

    @Column(name = "billing_state_code", length = 10)
    private String stateCode;

    @Column(name = "billing_pincode", length = 20)
    private String pincode;

    @Column(name = "billing_country", length = 100)
    @Builder.Default
    private String country = "India";
}
