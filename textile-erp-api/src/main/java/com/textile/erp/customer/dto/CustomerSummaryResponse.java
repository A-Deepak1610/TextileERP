package com.textile.erp.customer.dto;

import com.textile.erp.customer.entity.CustomerStatus;
import java.math.BigDecimal;
import java.time.Instant;
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
public class CustomerSummaryResponse {

    private UUID id;
    private String customerCode;
    private String name;
    private String gstin;
    private String phone;
    private String email;
    private String city;
    private String state;
    private BigDecimal creditLimit;
    private CustomerStatus status;
    private int shippingPartyCount;
    private Instant createdAt;
}
