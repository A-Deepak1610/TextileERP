package com.textile.erp.invoice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(
    name = "invoices",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_invoices_tenant_number", columnNames = {"tenant_id", "invoice_number"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @Column(name = "financial_year", nullable = false, length = 10)
    private String financialYear;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "invoice_type", nullable = false, length = 30)
    private InvoiceType invoiceType = InvoiceType.TAX_INVOICE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 30)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "shipping_address_id")
    private UUID shippingAddressId;

    @Column(name = "agent_id")
    private UUID agentId;

    // Immutable JSON snapshots
    @Column(name = "seller_snapshot_json", columnDefinition = "TEXT")
    private String sellerSnapshotJson;

    @Column(name = "billing_snapshot_json", columnDefinition = "TEXT")
    private String billingSnapshotJson;

    @Column(name = "shipping_snapshot_json", columnDefinition = "TEXT")
    private String shippingSnapshotJson;

    @Column(name = "bank_snapshot_json", columnDefinition = "TEXT")
    private String bankSnapshotJson;

    @Column(name = "agent_name_snapshot")
    private String agentNameSnapshot;

    @Column(name = "terms_and_conditions_snapshot", columnDefinition = "TEXT")
    private String termsAndConditionsSnapshot;

    // Financial totals
    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cgst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal cgstRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cgst_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "sgst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal sgstRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "sgst_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "igst_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal igstRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "igst_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "round_off_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal roundOffAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "grand_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "amount_in_words", columnDefinition = "TEXT")
    private String amountInWords;

    // Payment terms & tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode", length = 30)
    private PaymentMode paymentMode;

    @Builder.Default
    @Column(name = "credit_days")
    private Integer creditDays = 0;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "payment_terms_note", columnDefinition = "TEXT")
    private String paymentTermsNote;

    @Builder.Default
    @Column(name = "total_paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPaidAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "due_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal dueAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    // Agent Commission
    @Column(name = "commission_type", length = 20)
    private String commissionType;

    @Builder.Default
    @Column(name = "commission_value", precision = 10, scale = 2)
    private BigDecimal commissionValue = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "commission_amount", precision = 15, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "agent_notes", columnDefinition = "TEXT")
    private String agentNotes;

    // Audit & lifecycle
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoicePayment> payments = new ArrayList<>();

    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }

    public void addPayment(InvoicePayment payment) {
        payments.add(payment);
        payment.setInvoice(this);
    }
}
