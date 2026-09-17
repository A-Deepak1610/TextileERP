package com.textile.erp.customer.dto;

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
public class BillingAddressDto {
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String district;
    private String state;
    private String stateCode;
    private String pincode;
    private String country;
}
