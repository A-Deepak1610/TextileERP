package com.textile.erp.invoice.dto;

import com.textile.erp.invoice.entity.PaymentMode;
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
public class PaymentResponse {
    private UUID id;
    private UUID tenantId;
    private UUID invoiceId;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private PaymentMode paymentMode;
    private String referenceNumber;
    private String notes;
    private Instant createdAt;
}
