package com.textile.erp.invoice.dto;

import com.textile.erp.agent.entity.CommissionType;
import com.textile.erp.invoice.entity.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
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
public class UpdateInvoiceRequest {

    private LocalDate invoiceDate;
    private UUID shippingAddressId;
    private Boolean sameAsBilling;

    private UUID agentId;
    private CommissionType commissionType;
    private BigDecimal commissionValue;
    private String agentNotes;

    private PaymentMode paymentMode;
    private Integer creditDays;
    private String paymentTermsNote;

    @NotEmpty(message = "At least one invoice item is required")
    @Valid
    private List<InvoiceItemRequest> items;
}
