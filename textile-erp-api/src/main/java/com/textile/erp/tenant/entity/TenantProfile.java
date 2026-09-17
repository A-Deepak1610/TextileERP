package com.textile.erp.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "tenant_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantProfile {

    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Embedded
    @Builder.Default
    private CompanyIdentity companyIdentity = new CompanyIdentity();

    @Embedded
    @Builder.Default
    private Address registeredAddress = new Address();

    @Embedded
    @Builder.Default
    private ContactInfo contactInfo = new ContactInfo();

    @Embedded
    @Builder.Default
    private TaxInfo taxInfo = new TaxInfo();

    @Embedded
    @Builder.Default
    private BankInfo bankInfo = new BankInfo();

    @Embedded
    @Builder.Default
    private TextileBusinessInfo textileBusinessInfo = new TextileBusinessInfo();

    @Embedded
    @Builder.Default
    private InvoiceConfig invoiceConfig = new InvoiceConfig();

    @Embedded
    @Builder.Default
    private ErpSettings erpSettings = new ErpSettings();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
