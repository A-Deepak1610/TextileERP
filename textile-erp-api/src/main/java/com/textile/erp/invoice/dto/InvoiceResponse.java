package com.textile.erp.invoice.dto;

import com.textile.erp.invoice.entity.InvoiceStatus;
import com.textile.erp.invoice.entity.InvoiceType;
import com.textile.erp.invoice.entity.PaymentMode;
import com.textile.erp.invoice.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
public class InvoiceResponse {

    private UUID id;
    private UUID tenantId;
    private String invoiceNumber;
    private String financialYear;
    private LocalDate invoiceDate;
    private InvoiceType invoiceType;
    private InvoiceStatus status;

    private UUID customerId;
    private UUID shippingAddressId;
    private UUID agentId;

    private SellerSnapshot sellerSnapshot;
    private BillingSnapshot billingSnapshot;
    private ShippingSnapshot shippingSnapshot;
    private BankSnapshot bankSnapshot;
    private String agentNameSnapshot;
    private String termsAndConditionsSnapshot;

    private BigDecimal subtotal;
    private BigDecimal cgstRate;
    private BigDecimal cgstAmount;
    private BigDecimal sgstRate;
    private BigDecimal sgstAmount;
    private BigDecimal igstRate;
    private BigDecimal igstAmount;
    private BigDecimal roundOffAmount;
    private BigDecimal grandTotal;
    private String amountInWords;

    private PaymentMode paymentMode;
    private Integer creditDays;
    private LocalDate dueDate;
    private String paymentTermsNote;
    private BigDecimal totalPaidAmount;
    private BigDecimal dueAmount;
    private PaymentStatus paymentStatus;

    private String commissionType;
    private BigDecimal commissionValue;
    private BigDecimal commissionAmount;
    private String agentNotes;

    private String cancellationReason;
    private String pdfUrl;
    private UUID createdBy;
    private Instant issuedAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;

    private List<InvoiceItemResponse> items;
    private List<PaymentResponse> payments;
}
