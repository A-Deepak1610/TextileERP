package com.textile.erp.invoice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "description_snapshot", columnDefinition = "TEXT")
    private String descriptionSnapshot;

    @Column(name = "hsn_code_snapshot", length = 20)
    private String hsnCodeSnapshot;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String unit = "METER";

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal meters;

    @Builder.Default
    @Column(name = "folding_less_percent", nullable = false, precision = 6, scale = 3)
    private BigDecimal foldingLessPercent = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "folding_less_meters", nullable = false, precision = 12, scale = 3)
    private BigDecimal foldingLessMeters = BigDecimal.ZERO;

    @Column(name = "total_meters", nullable = false, precision = 12, scale = 3)
    private BigDecimal totalMeters;

    @Column(name = "rate_per_meter", nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerMeter;

    @Column(name = "taxable_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableAmount;

    @Column(name = "bale_details", columnDefinition = "TEXT")
    private String baleDetails;

    @Builder.Default
    @Column(name = "total_bales")
    private Integer totalBales = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "invoiceItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceBale> bales = new ArrayList<>();

    public void addBale(InvoiceBale bale) {
        bales.add(bale);
        bale.setInvoiceItem(this);
    }
}
