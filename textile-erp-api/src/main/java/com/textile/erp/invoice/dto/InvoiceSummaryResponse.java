package com.textile.erp.invoice.dto;

import com.textile.erp.invoice.entity.InvoiceStatus;
import com.textile.erp.invoice.entity.InvoiceType;
import com.textile.erp.invoice.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
public class InvoiceSummaryResponse {
    private UUID id;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private InvoiceType invoiceType;
    private InvoiceStatus status;
    private UUID customerId;
    private String customerName;
    private String agentName;
    private BigDecimal subtotal;
    private BigDecimal grandTotal;
    private BigDecimal totalPaidAmount;
    private BigDecimal dueAmount;
    private PaymentStatus paymentStatus;
    private Integer itemCount;
    private Instant createdAt;
}
