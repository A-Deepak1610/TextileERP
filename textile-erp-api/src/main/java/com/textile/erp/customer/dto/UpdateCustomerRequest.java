package com.textile.erp.customer.dto;

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
public class UpdateCustomerRequest {

    private String customerCode;
    private String name;
    private String gstin;
    private String pan;
    private String phone;
    private String email;
    private BillingAddressDto billingAddress;
    private Integer paymentTermsDays;
    private BigDecimal creditLimit;
}
