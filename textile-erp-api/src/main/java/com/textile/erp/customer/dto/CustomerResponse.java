package com.textile.erp.customer.dto;

import com.textile.erp.customer.entity.CustomerStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
public class CustomerResponse {

    private UUID id;
    private UUID tenantId;
    private String customerCode;
    private String name;
    private String gstin;
    private String pan;
    private String phone;
    private String email;
    private BillingAddressDto billingAddress;
    private Integer paymentTermsDays;
    private BigDecimal creditLimit;
    private CustomerStatus status;
    private List<ShippingPartyResponse> shippingParties;
    private Instant createdAt;
    private Instant updatedAt;
}
