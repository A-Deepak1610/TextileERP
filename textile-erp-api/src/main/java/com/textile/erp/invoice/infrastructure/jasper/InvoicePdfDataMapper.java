package com.textile.erp.invoice.infrastructure.jasper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.textile.erp.invoice.dto.BankSnapshot;
import com.textile.erp.invoice.dto.BillingSnapshot;
import com.textile.erp.invoice.dto.SellerSnapshot;
import com.textile.erp.invoice.dto.ShippingSnapshot;
import com.textile.erp.invoice.entity.Invoice;
import com.textile.erp.invoice.entity.InvoiceItem;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoicePdfDataMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ObjectMapper objectMapper;

    public InvoicePdfData mapToPdfData(Invoice invoice) {
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice cannot be null");
        }

        SellerSnapshot seller = fromJson(invoice.getSellerSnapshotJson(), SellerSnapshot.class);
        BillingSnapshot billing = fromJson(invoice.getBillingSnapshotJson(), BillingSnapshot.class);
        ShippingSnapshot shipping = fromJson(invoice.getShippingSnapshotJson(), ShippingSnapshot.class);
        BankSnapshot bank = fromJson(invoice.getBankSnapshotJson(), BankSnapshot.class);

        List<InvoiceItemPdfData> itemRows = new ArrayList<>();
        int serial = 1;
        int totalBaleCount = 0;
        StringBuilder allBalesSummary = new StringBuilder();

        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                if (item.getTotalBales() != null && item.getTotalBales() > 0) {
                    totalBaleCount += item.getTotalBales();
                }
                if (item.getBaleDetails() != null && !item.getBaleDetails().isBlank()) {
                    if (allBalesSummary.length() > 0) {
                        allBalesSummary.append(", ");
                    }
                    allBalesSummary.append(item.getBaleDetails());
                }

                itemRows.add(InvoiceItemPdfData.builder()
                        .serialNumber(serial++)
                        .description(item.getDescription() != null ? item.getDescription() : "")
                        .hsnCode(item.getHsnCode() != null ? item.getHsnCode() : "")
                        .meters(formatMeters(item.getMeters()))
                        .foldingLessPercent(formatPercent(item.getFoldingLessPercent()))
                        .foldingLessMeters(formatMeters(item.getFoldingLessMeters()))
                        .totalMeters(formatMeters(item.getTotalMeters()))
                        .rate(formatPlainMoney(item.getRatePerMeter()))
                        .taxableAmount(formatPlainMoney(item.getTaxableAmount()))
                        .baleDetails(item.getBaleDetails() != null ? item.getBaleDetails() : "")
                        .build());
            }
        }

        String formattedDate = invoice.getInvoiceDate() != null
                ? invoice.getInvoiceDate().format(DATE_FORMATTER)
                : "";

        String terms = invoice.getTermsAndConditionsSnapshot() != null && !invoice.getTermsAndConditionsSnapshot().isBlank()
                ? invoice.getTermsAndConditionsSnapshot()
                : "1. No claims will be accepted once goods are processed or altered.\n"
                + "2. Transit loss or damage is not our responsibility; risk lies with the buyer.\n"
                + "3. All disputes are subject to local jurisdiction only.";

        String paymentTerms = invoice.getPaymentTermsNote() != null
                ? invoice.getPaymentTermsNote()
                : (invoice.getCreditDays() != null ? invoice.getCreditDays() + " Days Credit" : "Payment on delivery");

        return InvoicePdfData.builder()
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceDate(formattedDate)
                .invoiceType("TAX INVOICE")
                .sellerName(seller != null && seller.getCompanyName() != null ? seller.getCompanyName() : "Textile Company")
                .sellerAddress(seller != null ? seller.getAddress() : "")
                .sellerGstin(seller != null && seller.getGstin() != null ? seller.getGstin() : "")
                .sellerState(seller != null && seller.getState() != null ? seller.getState() : "")
                .sellerPhone(seller != null && seller.getPhone() != null ? seller.getPhone() : "")
                .sellerEmail(seller != null && seller.getEmail() != null ? seller.getEmail() : "")
                .logoPath(null)
                .billingName(billing != null && billing.getCustomerName() != null ? billing.getCustomerName() : "")
                .billingAddress(billing != null && billing.getBillingAddress() != null ? billing.getBillingAddress() : "")
                .billingGstin(billing != null && billing.getGstin() != null ? billing.getGstin() : "")
                .billingState(billing != null && billing.getState() != null ? billing.getState() : "")
                .shippingName(shipping != null && shipping.getShippingPartyName() != null ? shipping.getShippingPartyName() : (billing != null ? billing.getCustomerName() : ""))
                .shippingAddress(shipping != null && shipping.getShippingAddress() != null ? shipping.getShippingAddress() : (billing != null ? billing.getBillingAddress() : ""))
                .shippingGstin(shipping != null && shipping.getGstin() != null ? shipping.getGstin() : (billing != null ? billing.getGstin() : ""))
                .shippingState(shipping != null && shipping.getState() != null ? shipping.getState() : (billing != null ? billing.getState() : ""))
                .items(itemRows)
                .totalBales(totalBaleCount > 0 ? String.valueOf(totalBaleCount) : "")
                .baleDetails(allBalesSummary.toString())
                .bankAccountName(seller != null && seller.getBankAccountName() != null ? seller.getBankAccountName() : "")
                .bankAccountNumber(seller != null && seller.getBankAccountNumber() != null ? seller.getBankAccountNumber() : (bank != null ? bank.getBankAccountNumber() : ""))
                .bankName(seller != null && seller.getBankName() != null ? seller.getBankName() : (bank != null ? bank.getBankName() : ""))
                .ifsc(seller != null && seller.getIfsc() != null ? seller.getIfsc() : (bank != null ? bank.getIfsc() : ""))
                .branch(seller != null && seller.getBranch() != null ? seller.getBranch() : (bank != null ? bank.getBranch() : ""))
                .taxableAmount(formatPlainMoney(invoice.getSubtotal()))
                .cgstRate(formatPercent(invoice.getCgstRate()))
                .cgstAmount(isZeroOrNull(invoice.getCgstAmount()) ? "-" : formatPlainMoney(invoice.getCgstAmount()))
                .sgstRate(formatPercent(invoice.getSgstRate()))
                .sgstAmount(isZeroOrNull(invoice.getSgstAmount()) ? "-" : formatPlainMoney(invoice.getSgstAmount()))
                .igstRate(formatPercent(invoice.getIgstRate()))
                .igstAmount(isZeroOrNull(invoice.getIgstAmount()) ? "-" : formatPlainMoney(invoice.getIgstAmount()))
                .roundOffAmount(formatPlainMoney(invoice.getRoundOffAmount()))
                .grandTotal(formatPlainMoney(invoice.getGrandTotal()))
                .amountInWords(invoice.getAmountInWords() != null ? invoice.getAmountInWords() : "")
                .paymentTerms(paymentTerms)
                .agentName(invoice.getAgentNameSnapshot() != null ? invoice.getAgentNameSnapshot() : "")
                .termsAndConditions(terms)
                .authorizedSignatoryName(seller != null && seller.getCompanyName() != null ? seller.getCompanyName() : "Authorized Signatory")
                .build();
    }

    public String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "₹0.00";
        }
        return String.format(Locale.US, "₹%,.2f", amount);
    }

    public String formatPlainMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return String.format(Locale.US, "%,.2f", amount);
    }

    public String formatMeters(BigDecimal meters) {
        if (meters == null) {
            return "0.00";
        }
        return String.format(Locale.US, "%,.2f", meters);
    }

    public String formatPercent(BigDecimal pct) {
        if (pct == null) {
            return "0.00%";
        }
        return String.format(Locale.US, "%.2f%%", pct);
    }

    private boolean isZeroOrNull(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("Failed to deserialize snapshot into {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }
}
