package com.textile.erp.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactInfo {

    @Column(name = "primary_email")
    private String primaryEmail;

    @Column(name = "primary_phone", length = 50)
    private String primaryPhone;

    @Column(name = "secondary_phone", length = 50)
    private String secondaryPhone;

    @Column(name = "support_email")
    private String supportEmail;
}
