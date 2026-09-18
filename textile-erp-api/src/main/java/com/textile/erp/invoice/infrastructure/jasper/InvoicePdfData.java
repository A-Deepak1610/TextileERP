package com.textile.erp.invoice.infrastructure.jasper;

import java.util.List;
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
public class InvoicePdfData {

    private String invoiceNumber;
    private String invoiceDate;
    private String invoiceType;

    // Seller Details
    private String sellerName;
    private String sellerAddress;
    private String sellerGstin;
    private String sellerState;
    private String sellerPhone;
    private String sellerEmail;
    private String logoPath;

    // Billed To (Buyer)
    private String billingName;
    private String billingAddress;
    private String billingGstin;
    private String billingState;

    // Shipped To (Consignee)
    private String shippingName;
    private String shippingAddress;
    private String shippingGstin;
    private String shippingState;

    // Line Items
    private List<InvoiceItemPdfData> items;

    // Bales & Packaging
    private String totalBales;
    private String baleDetails;

    // Bank Details
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankName;
    private String ifsc;
    private String branch;

    // Financial Totals
    private String taxableAmount;
    private String cgstRate;
    private String cgstAmount;
    private String sgstRate;
    private String sgstAmount;
    private String igstRate;
    private String igstAmount;
    private String roundOffAmount;
    private String grandTotal;
    private String amountInWords;

    // Additional Notes
    private String paymentTerms;
    private String agentName;
    private String termsAndConditions;
    private String authorizedSignatoryName;
}
