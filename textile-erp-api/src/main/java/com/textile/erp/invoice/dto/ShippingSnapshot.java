package com.textile.erp.invoice.dto;

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
public class ShippingSnapshot {
    private Boolean sameAsBilling;
    private UUID shippingAddressId;
    private String shippingPartyName;
    private String gstin;
    private String pan;
    private String shippingAddress;
    private String city;
    private String state;
    private String stateCode;
    private String pincode;
    private String contactPerson;
    private String phone;
    private String email;
}
