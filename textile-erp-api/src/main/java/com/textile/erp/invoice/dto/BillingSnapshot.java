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
public class BillingSnapshot {
    private UUID customerId;
    private String customerCode;
    private String customerName;
    private String gstin;
    private String pan;
    private String billingAddress;
    private String city;
    private String state;
    private String stateCode;
    private String pincode;
    private String phone;
    private String email;
}
