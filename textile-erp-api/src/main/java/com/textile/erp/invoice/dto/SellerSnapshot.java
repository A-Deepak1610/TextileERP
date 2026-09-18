package com.textile.erp.invoice.dto;

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
public class SellerSnapshot {
    private String companyName;
    private String address;
    private String gstin;
    private String pan;
    private String state;
    private String stateCode;
    private String phone;
    private String email;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankName;
    private String ifsc;
    private String branch;
}
