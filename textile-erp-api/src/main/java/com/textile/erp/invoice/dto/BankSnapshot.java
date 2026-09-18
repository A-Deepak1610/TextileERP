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
public class BankSnapshot {
    private String bankName;
    private String bankAccountNumber;
    private String ifsc;
    private String branch;
    private String accountType;
    private String upiId;
}
