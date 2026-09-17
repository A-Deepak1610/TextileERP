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
public class InvoiceConfig {

    @Column(name = "invoice_prefix", length = 20)
    @Builder.Default
    private String invoicePrefix = "INV-";

    @Column(name = "invoice_starting_sequence")
    @Builder.Default
    private Long invoiceStartingSequence = 1L;

    @Column(name = "default_payment_terms_days")
    @Builder.Default
    private Integer defaultPaymentTermsDays = 30;

    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions;

    @Column(name = "declaration_text", columnDefinition = "TEXT")
    private String declarationText;

    @Column(name = "signature_image_url", length = 500)
    private String signatureImageUrl;
}
