package com.textile.erp.invoice.infrastructure.jasper;

import com.textile.erp.invoice.domain.InvoicePdfGenerationException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JasperInvoicePdfGenerator {

    private static final String TEMPLATE_PATH = "/reports/invoice.jrxml";
    private final AtomicReference<JasperReport> compiledReportCache = new AtomicReference<>();

    public byte[] generatePdf(InvoicePdfData data) {
        if (data == null) {
            throw new IllegalArgumentException("InvoicePdfData cannot be null");
        }

        try {
            JasperReport jasperReport = getOrCompileReport();

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("invoiceNumber", defaultString(data.getInvoiceNumber()));
            parameters.put("invoiceDate", defaultString(data.getInvoiceDate()));
            parameters.put("invoiceType", defaultString(data.getInvoiceType()));

            parameters.put("sellerName", defaultString(data.getSellerName()));
            parameters.put("sellerAddress", defaultString(data.getSellerAddress()));
            parameters.put("sellerGstin", defaultString(data.getSellerGstin()));
            parameters.put("sellerState", defaultString(data.getSellerState()));
            parameters.put("sellerPhone", defaultString(data.getSellerPhone()));
            parameters.put("sellerEmail", defaultString(data.getSellerEmail()));

            parameters.put("billingName", defaultString(data.getBillingName()));
            parameters.put("billingAddress", defaultString(data.getBillingAddress()));
            parameters.put("billingGstin", defaultString(data.getBillingGstin()));
            parameters.put("billingState", defaultString(data.getBillingState()));

            parameters.put("shippingName", defaultString(data.getShippingName()));
            parameters.put("shippingAddress", defaultString(data.getShippingAddress()));
            parameters.put("shippingGstin", defaultString(data.getShippingGstin()));
            parameters.put("shippingState", defaultString(data.getShippingState()));

            parameters.put("totalBales", defaultString(data.getTotalBales()));
            parameters.put("baleDetails", defaultString(data.getBaleDetails()));

            parameters.put("bankAccountName", defaultString(data.getBankAccountName()));
            parameters.put("bankAccountNumber", defaultString(data.getBankAccountNumber()));
            parameters.put("bankName", defaultString(data.getBankName()));
            parameters.put("ifsc", defaultString(data.getIfsc()));
            parameters.put("branch", defaultString(data.getBranch()));

            parameters.put("taxableAmount", defaultString(data.getTaxableAmount()));
            parameters.put("cgstRate", defaultString(data.getCgstRate()));
            parameters.put("cgstAmount", defaultString(data.getCgstAmount()));
            parameters.put("sgstRate", defaultString(data.getSgstRate()));
            parameters.put("sgstAmount", defaultString(data.getSgstAmount()));
            parameters.put("igstRate", defaultString(data.getIgstRate()));
            parameters.put("igstAmount", defaultString(data.getIgstAmount()));
            parameters.put("roundOffAmount", defaultString(data.getRoundOffAmount()));
            parameters.put("grandTotal", defaultString(data.getGrandTotal()));
            parameters.put("amountInWords", defaultString(data.getAmountInWords()));
            parameters.put("termsAndConditions", defaultString(data.getTermsAndConditions()));
            parameters.put("authorizedSignatoryName", defaultString(data.getAuthorizedSignatoryName()));

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(
                    data.getItems() != null ? data.getItems() : java.util.Collections.emptyList()
            );

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            log.info("Generated invoice PDF for {} (size: {} bytes)", data.getInvoiceNumber(), pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            log.error("Failed to generate JasperReports PDF for invoice {}: {}", data.getInvoiceNumber(), e.getMessage(), e);
            throw new InvoicePdfGenerationException("Failed to generate PDF for invoice: " + e.getMessage(), e);
        }
    }

    private JasperReport getOrCompileReport() {
        JasperReport cached = compiledReportCache.get();
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            cached = compiledReportCache.get();
            if (cached != null) {
                return cached;
            }

            try (InputStream in = getClass().getResourceAsStream(TEMPLATE_PATH)) {
                if (in == null) {
                    throw new IllegalStateException("Jasper template not found at: " + TEMPLATE_PATH);
                }
                log.info("Compiling JasperReports template from {}", TEMPLATE_PATH);
                JasperReport compiled = JasperCompileManager.compileReport(in);
                compiledReportCache.set(compiled);
                return compiled;
            } catch (Exception e) {
                throw new InvoicePdfGenerationException("Failed to compile JasperReports template: " + e.getMessage(), e);
            }
        }
    }

    private String defaultString(String val) {
        return val != null ? val : "";
    }
}
