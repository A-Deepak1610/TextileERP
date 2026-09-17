package com.textile.erp.tenant.dto;

import com.textile.erp.tenant.entity.Address;
import com.textile.erp.tenant.entity.BankInfo;
import com.textile.erp.tenant.entity.CompanyIdentity;
import com.textile.erp.tenant.entity.ContactInfo;
import com.textile.erp.tenant.entity.ErpSettings;
import com.textile.erp.tenant.entity.InvoiceConfig;
import com.textile.erp.tenant.entity.TaxInfo;
import com.textile.erp.tenant.entity.TextileBusinessInfo;
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
public class UpdateTenantProfileRequest {

    private CompanyIdentity companyIdentity;
    private Address registeredAddress;
    private ContactInfo contactInfo;
    private TaxInfo taxInfo;
    private BankInfo bankInfo;
    private TextileBusinessInfo textileBusinessInfo;
    private InvoiceConfig invoiceConfig;
    private ErpSettings erpSettings;
}
