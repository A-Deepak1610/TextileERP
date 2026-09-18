package com.textile.erp.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.textile.erp.invoice.dto.BankSnapshot;
import com.textile.erp.invoice.dto.BillingSnapshot;
import com.textile.erp.invoice.dto.SellerSnapshot;
import com.textile.erp.invoice.dto.ShippingSnapshot;
import com.textile.erp.invoice.entity.Invoice;
import com.textile.erp.invoice.entity.InvoiceItem;
import com.textile.erp.invoice.entity.InvoiceStatus;
import com.textile.erp.invoice.infrastructure.jasper.InvoiceItemPdfData;
import com.textile.erp.invoice.infrastructure.jasper.InvoicePdfData;
import com.textile.erp.invoice.infrastructure.jasper.InvoicePdfDataMapper;
import com.textile.erp.invoice.infrastructure.jasper.JasperInvoicePdfGenerator;
import com.textile.erp.invoice.infrastructure.storage.LocalDocumentStorage;
import com.textile.erp.invoice.infrastructure.storage.StoredDocument;
import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

class InvoicePdfUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InvoicePdfDataMapper dataMapper = new InvoicePdfDataMapper(objectMapper);
    private final JasperInvoicePdfGenerator pdfGenerator = new JasperInvoicePdfGenerator();

    @Test
    @DisplayName("InvoicePdfDataMapper should correctly map invoice snapshots, textile items, and financial totals")
    void testDataMapper_successfulMapping() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        SellerSnapshot seller = SellerSnapshot.builder()
                .companyName("Murugan Tex")
                .address("3/68 Senan Kadu, Tiruppur")
                .gstin("33ABBFM6031B1ZZ")
                .state("Tamil Nadu")
                .phone("9442156066")
                .email("murugantex6576@gmail.com")
                .bankAccountName("Murugan Tex")
                .bankAccountNumber("1612261010137")
                .bankName("Canara Bank")
                .ifsc("CNRB0001612")
                .branch("Thekkalur")
                .build();

        BillingSnapshot billing = BillingSnapshot.builder()
                .customerName("Metro Global Exports LLP")
                .billingAddress("901 Links Building, Khar West, Mumbai")
                .gstin("27ABHFM7043F1Z8")
                .state("Maharashtra")
                .build();

        ShippingSnapshot shipping = ShippingSnapshot.builder()
                .shippingPartyName("Payal Process")
                .shippingAddress("Nilkanth Udhyog Nagar, Jetpur, Rajkot")
                .gstin("24AWXPS5026M1ZQ")
                .state("Gujarat")
                .build();

        InvoiceItem item = InvoiceItem.builder()
                .descriptionSnapshot("20X20/60X48/50\" 180GM")
                .hsnCodeSnapshot("520811")
                .meters(new BigDecimal("30396.250"))
                .foldingLessPercent(BigDecimal.ZERO)
                .foldingLessMeters(BigDecimal.ZERO)
                .totalMeters(new BigDecimal("30396.250"))
                .ratePerMeter(new BigDecimal("38.50"))
                .taxableAmount(new BigDecimal("1170255.63"))
                .baleDetails("494-545")
                .totalBales(52)
                .build();

        Invoice invoice = Invoice.builder()
                .id(invoiceId)
                .tenantId(tenantId)
                .invoiceNumber("SAL/2026-27/0017")
                .invoiceDate(LocalDate.of(2026, 9, 12))
                .status(InvoiceStatus.ISSUED)
                .sellerSnapshotJson(objectMapper.writeValueAsString(seller))
                .billingSnapshotJson(objectMapper.writeValueAsString(billing))
                .shippingSnapshotJson(objectMapper.writeValueAsString(shipping))
                .bankSnapshotJson(objectMapper.writeValueAsString(new BankSnapshot()))
                .subtotal(new BigDecimal("1170255.63"))
                .cgstRate(BigDecimal.ZERO)
                .cgstAmount(BigDecimal.ZERO)
                .sgstRate(BigDecimal.ZERO)
                .sgstAmount(BigDecimal.ZERO)
                .igstRate(new BigDecimal("5.00"))
                .igstAmount(new BigDecimal("58512.78"))
                .roundOffAmount(new BigDecimal("-0.41"))
                .grandTotal(new BigDecimal("1228768.00"))
                .amountInWords("Twelve Lakh Twenty Eight Thousand Seven Hundred Sixty Eight Only")
                .items(List.of(item))
                .build();

        InvoicePdfData data = dataMapper.mapToPdfData(invoice);

        assertThat(data.getInvoiceNumber()).isEqualTo("SAL/2026-27/0017");
        assertThat(data.getInvoiceDate()).isEqualTo("12/09/2026");
        assertThat(data.getSellerName()).isEqualTo("Murugan Tex");
        assertThat(data.getSellerGstin()).isEqualTo("33ABBFM6031B1ZZ");
        assertThat(data.getBillingName()).isEqualTo("Metro Global Exports LLP");
        assertThat(data.getBillingGstin()).isEqualTo("27ABHFM7043F1Z8");
        assertThat(data.getShippingName()).isEqualTo("Payal Process");
        assertThat(data.getShippingGstin()).isEqualTo("24AWXPS5026M1ZQ");
        assertThat(data.getBankAccountNumber()).isEqualTo("1612261010137");

        assertThat(data.getItems()).hasSize(1);
        InvoiceItemPdfData row = data.getItems().get(0);
        assertThat(row.getDescription()).isEqualTo("20X20/60X48/50\" 180GM");
        assertThat(row.getHsnCode()).isEqualTo("520811");
        assertThat(row.getMeters()).contains("30,396.25");
        assertThat(row.getRate()).isEqualTo("38.50");
        assertThat(row.getTaxableAmount()).contains("1,170,255.63");

        assertThat(data.getTotalBales()).isEqualTo("52");
        assertThat(data.getBaleDetails()).isEqualTo("494-545");
        assertThat(data.getIgstAmount()).isEqualTo("58,512.78");
        assertThat(data.getCgstAmount()).isEqualTo("-");
        assertThat(data.getSgstAmount()).isEqualTo("-");
        assertThat(data.getGrandTotal()).isEqualTo("1,228,768.00");
        assertThat(data.getAmountInWords()).contains("Twelve Lakh");
    }

    @Test
    @DisplayName("JasperInvoicePdfGenerator should compile invoice.jrxml and produce valid PDF bytes starting with %PDF")
    void testPdfGenerator_generatesValidPdfBytes() {
        InvoiceItemPdfData item1 = InvoiceItemPdfData.builder()
                .serialNumber(1)
                .description("Cotton Grey Fabric 40s")
                .hsnCode("5208")
                .meters("1,000.00")
                .foldingLessPercent("2.00%")
                .foldingLessMeters("20.00")
                .totalMeters("980.00")
                .rate("45.50")
                .taxableAmount("44,590.00")
                .baleDetails("B-1 to B-5")
                .build();

        InvoicePdfData data = InvoicePdfData.builder()
                .invoiceNumber("SAL/2026-27/0001")
                .invoiceDate("18/09/2026")
                .invoiceType("TAX INVOICE")
                .sellerName("Murugan Tex")
                .sellerAddress("3/68 Senan Kadu, Tiruppur")
                .sellerGstin("33ABBFM6031B1ZZ")
                .sellerState("Tamil Nadu")
                .sellerPhone("9442156066")
                .sellerEmail("murugantex6576@gmail.com")
                .billingName("Sri Lakshmi Traders")
                .billingAddress("14 Karur Byepass Road, Erode")
                .billingGstin("33AABCS1429B1ZB")
                .billingState("Tamil Nadu")
                .shippingName("Sri Lakshmi Traders")
                .shippingAddress("14 Karur Byepass Road, Erode")
                .shippingGstin("33AABCS1429B1ZB")
                .shippingState("Tamil Nadu")
                .items(List.of(item1))
                .totalBales("5")
                .baleDetails("B-1 to B-5")
                .bankAccountName("Murugan Tex")
                .bankAccountNumber("1612261010137")
                .bankName("Canara Bank")
                .ifsc("CNRB0001612")
                .branch("Thekkalur")
                .taxableAmount("44,590.00")
                .cgstRate("2.50%")
                .cgstAmount("1,114.75")
                .sgstRate("2.50%")
                .sgstAmount("1,114.75")
                .igstRate("0.00%")
                .igstAmount("-")
                .roundOffAmount("0.50")
                .grandTotal("46,820.00")
                .amountInWords("Rupees Forty Six Thousand Eight Hundred Twenty Only")
                .termsAndConditions("1. Goods once sold will not be taken back.")
                .authorizedSignatoryName("Murugan Tex")
                .build();

        byte[] pdfBytes = pdfGenerator.generatePdf(data);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(1000);

        // Standard PDF magic header: %PDF
        String header = new String(pdfBytes, 0, 4, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF");

        // Export sample PDF for visual inspection
        try {
            Files.write(Path.of("../sample_invoice.pdf"), pdfBytes);
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("LocalDocumentStorage should store, load, checksum, and reject path traversal")
    void testLocalDocumentStorage(@TempDir Path tempDir) throws Exception {
        LocalDocumentStorage storage = new LocalDocumentStorage(tempDir.toString());

        String key = "tenant/uuid-1/invoices/2026-27/SAL-001.pdf";
        byte[] content = "Fake PDF content for test".getBytes(StandardCharsets.UTF_8);

        // 1. Store
        StoredDocument stored = storage.store(key, content, "application/pdf");
        assertThat(stored.getStorageKey()).isEqualTo(key);
        assertThat(stored.getFileName()).isEqualTo("SAL-001.pdf");
        assertThat(stored.getFileSize()).isEqualTo((long) content.length);
        assertThat(stored.getChecksumSha256()).isNotBlank();

        // 2. Exists & Load
        assertThat(storage.exists(key)).isTrue();
        Resource resource = storage.load(key);
        assertThat(resource.exists()).isTrue();
        assertThat(resource.contentLength()).isEqualTo(content.length);

        // 3. Path traversal protection
        assertThatThrownBy(() -> storage.store("../../etc/malicious.pdf", content, "application/pdf"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Path traversal attempt detected");

        assertThatThrownBy(() -> storage.load("../../etc/passwd"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Path traversal attempt detected");
    }
}
