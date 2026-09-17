package com.textile.erp.customer.dto;

import java.math.BigDecimal;
import java.util.List;
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
public class CreateCustomerRequest {

    private String customerCode;
    private String name;
    private String gstin;
    private String pan;
    private String phone;
    private String email;
    private BillingAddressDto billingAddress;
    private Integer paymentTermsDays;
    private BigDecimal creditLimit;

    // Shipping party options during creation
    @Builder.Default
    private Boolean sameAsBilling = false;
    private List<CreateShippingPartyRequest> shippingParties;
}
