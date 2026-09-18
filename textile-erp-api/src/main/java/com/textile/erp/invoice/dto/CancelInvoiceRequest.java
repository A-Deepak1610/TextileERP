package com.textile.erp.invoice.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CancelInvoiceRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String cancellationReason;
}
