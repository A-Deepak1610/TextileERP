package com.textile.erp.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.textile.erp.auth.service.JwtService;
import com.textile.erp.customer.dto.BillingAddressDto;
import com.textile.erp.customer.dto.CreateCustomerRequest;
import com.textile.erp.customer.dto.CustomerResponse;
import com.textile.erp.invoice.domain.InvoicePdfStatus;
import com.textile.erp.invoice.dto.CreateInvoiceRequest;
import com.textile.erp.invoice.dto.InvoiceBaleRequest;
import com.textile.erp.invoice.dto.InvoiceDocumentResponse;
import com.textile.erp.invoice.dto.InvoiceItemRequest;
import com.textile.erp.invoice.dto.InvoiceResponse;
import com.textile.erp.invoice.entity.PaymentMode;
import com.textile.erp.invoice.infrastructure.outbox.InvoicePdfOutboxConsumer;
import com.textile.erp.invoice.infrastructure.outbox.OutboxEventEntity;
import com.textile.erp.invoice.infrastructure.outbox.OutboxEventRepository;
import com.textile.erp.invoice.persistence.InvoiceDocumentEntity;
import com.textile.erp.invoice.persistence.InvoiceDocumentRepository;
import com.textile.erp.product.dto.CreateProductRequest;
import com.textile.erp.product.dto.ProductResponse;
import com.textile.erp.user.dto.TenantRequestDto;
import com.textile.erp.user.dto.TenantResponseDto;
import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.User;
import com.textile.erp.user.entity.UserStatus;
import com.textile.erp.user.repository.UserRepository;
import com.textile.erp.user.service.TenantService;
import com.textile.erp.user.service.UserService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class InvoicePdfIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private InvoiceDocumentRepository invoiceDocumentRepository;

    @Autowired
    private InvoicePdfOutboxConsumer invoicePdfOutboxConsumer;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    private UUID tenantAId;
    private UUID tenantBId;

    private String tokenA;
    private String tokenB;

    private UUID customerAId;
    private UUID productAId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        long ts = System.currentTimeMillis();

        TenantResponseDto tenantA = tenantService.createTenant(TenantRequestDto.builder()
                .name("Jasper Tex A " + ts)
                .slug("jasper-tex-a-" + ts)
                .build());
        tenantAId = tenantA.getId();

        TenantResponseDto tenantB = tenantService.createTenant(TenantRequestDto.builder()
                .name("Jasper Tex B " + ts)
                .slug("jasper-tex-b-" + ts)
                .build());
        tenantBId = tenantB.getId();

        User adminA = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantAId)
                .email("admin." + ts + "@jaspertexa.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Jasper")
                .lastName("AdminA")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminA.getId(), RoleName.TENANT_ADMIN);

        User adminB = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantBId)
                .email("admin." + ts + "@jaspertexb.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Jasper")
                .lastName("AdminB")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminB.getId(), RoleName.TENANT_ADMIN);

        tokenA = jwtService.generateAccessToken(adminA.getId(), tenantAId, adminA.getEmail(), Set.of(RoleName.TENANT_ADMIN));
        tokenB = jwtService.generateAccessToken(adminB.getId(), tenantBId, adminB.getEmail(), Set.of(RoleName.TENANT_ADMIN));

        // Create Customer
        CreateCustomerRequest custReq = CreateCustomerRequest.builder()
                .customerCode("CUST-PDF-" + ts)
                .name("Metro Global Exports LLP")
                .gstin("27ABHFM7043F1Z8")
                .pan("ABHFM7043F")
                .phone("9876543210")
                .email("metro@globalexports.com")
                .billingAddress(BillingAddressDto.builder()
                        .addressLine1("901 Links Building")
                        .city("Mumbai")
                        .state("Maharashtra")
                        .pincode("400052")
                        .country("India")
                        .build())
                .paymentTermsDays(30)
                .creditLimit(new BigDecimal("1000000.00"))
                .sameAsBilling(true)
                .build();

        String custRes = mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(custReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        CustomerResponse customer = objectMapper.readValue(custRes, CustomerResponse.class);
        customerAId = customer.getId();

        // Create Product
        CreateProductRequest prodReq = CreateProductRequest.builder()
                .productCode("PROD-GREY-" + ts)
                .productName("20X20/60X48/50\" 180GM")
                .hsnCode("520811")
                .fabricType("Cotton")
                .color("Grey")
                .gsm(180)
                .unit("METER")
                .defaultRatePerUnit(new BigDecimal("38.50"))
                .gstRate(new BigDecimal("5.00"))
                .build();

        String prodRes = mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        ProductResponse product = objectMapper.readValue(prodRes, ProductResponse.class);
        productAId = product.getId();
    }

    private InvoiceResponse createAndIssueSampleInvoice() throws Exception {
        InvoiceItemRequest item1 = InvoiceItemRequest.builder()
                .productId(productAId)
                .description("20X20/60X48/50\" 180GM")
                .hsnCode("520811")
                .meters(new BigDecimal("1000.000"))
                .foldingLessPercent(new BigDecimal("2.000"))
                .ratePerMeter(new BigDecimal("38.50"))
                .bales(List.of(InvoiceBaleRequest.builder()
                        .baleNumber("494-545")
                        .meters(new BigDecimal("1000.000"))
                        .pieceCount(52)
                        .build()))
                .build();

        CreateInvoiceRequest createReq = CreateInvoiceRequest.builder()
                .customerId(customerAId)
                .sameAsBilling(true)
                .invoiceDate(LocalDate.now())
                .paymentMode(PaymentMode.NEFT)
                .creditDays(30)
                .issueNow(true) // Immediately issue
                .items(List.of(item1))
                .build();

        String createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(createRes, InvoiceResponse.class);
    }

    @Test
    @DisplayName("Should generate invoice PDF via POST /api/invoices/{id}/pdf and stream via GET")
    void shouldGenerateAndDownloadInvoicePdf() throws Exception {
        InvoiceResponse invoice = createAndIssueSampleInvoice();

        // 1. Generate PDF
        String genRes = mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.documentType").value("INVOICE_PDF"))
                .andExpect(jsonPath("$.fileSize").isNumber())
                .andExpect(jsonPath("$.checksumSha256").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        InvoiceDocumentResponse docRes = objectMapper.readValue(genRes, InvoiceDocumentResponse.class);
        assertThat(docRes.getFileSize()).isGreaterThan(1000);

        // 2. Stream/Download PDF via GET
        MvcResult downloadResult = mockMvc.perform(get("/api/invoices/" + invoice.getId() + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION))
                .andReturn();

        byte[] downloadedBytes = downloadResult.getResponse().getContentAsByteArray();
        assertThat(downloadedBytes).isNotNull();
        assertThat((long) downloadedBytes.length).isEqualTo(docRes.getFileSize());

        // Validate %PDF magic bytes
        String magic = new String(downloadedBytes, 0, 4, StandardCharsets.US_ASCII);
        assertThat(magic).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("Should enforce idempotency: Generating twice without force=true returns same document")
    void shouldEnforceIdempotencyOnPdfGeneration() throws Exception {
        InvoiceResponse invoice = createAndIssueSampleInvoice();

        // First generation
        String res1 = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        InvoiceDocumentResponse doc1 = objectMapper.readValue(res1, InvoiceDocumentResponse.class);

        // Second generation without force
        String res2 = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        InvoiceDocumentResponse doc2 = objectMapper.readValue(res2, InvoiceDocumentResponse.class);

        assertThat(doc1.getId()).isEqualTo(doc2.getId());
        assertThat(doc1.getStorageKey()).isEqualTo(doc2.getStorageKey());
        assertThat(doc1.getChecksumSha256()).isEqualTo(doc2.getChecksumSha256());
    }

    @Test
    @DisplayName("Should reject PDF generation and download on DRAFT invoice with 409 Conflict")
    void shouldRejectPdfForDraftInvoice() throws Exception {
        InvoiceItemRequest item1 = InvoiceItemRequest.builder()
                .productId(productAId)
                .meters(new BigDecimal("100.000"))
                .ratePerMeter(new BigDecimal("50.00"))
                .build();

        CreateInvoiceRequest draftReq = CreateInvoiceRequest.builder()
                .customerId(customerAId)
                .sameAsBilling(true)
                .issueNow(false) // DRAFT
                .items(List.of(item1))
                .build();

        String draftRes = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        InvoiceResponse draft = objectMapper.readValue(draftRes, InvoiceResponse.class);

        // Attempt generate -> 409 Conflict
        mockMvc.perform(post("/api/invoices/" + draft.getId() + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isConflict());

        // Attempt download -> 409 Conflict
        mockMvc.perform(get("/api/invoices/" + draft.getId() + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should enforce strict tenant isolation: Tenant B cannot generate or download Tenant A's invoice PDF")
    void shouldEnforceTenantIsolationOnPdf() throws Exception {
        InvoiceResponse invoice = createAndIssueSampleInvoice();

        // Tenant A generates PDF
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Tenant B attempts generate on Tenant A's invoice -> 404
        mockMvc.perform(post("/api/invoices/" + invoice.getId() + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Tenant B attempts download on Tenant A's invoice -> 404
        mockMvc.perform(get("/api/invoices/" + invoice.getId() + "/pdf")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should insert outbox event on issue and process via outbox consumer")
    void shouldProcessOutboxEventToGeneratePdf() throws Exception {
        InvoiceResponse invoice = createAndIssueSampleInvoice();

        // Verify outbox event was recorded
        List<OutboxEventEntity> events = outboxEventRepository.findByTenantIdAndAggregateId(tenantAId, invoice.getId());
        assertThat(events).isNotEmpty();

        OutboxEventEntity event = events.get(0);
        assertThat(event.getEventType()).isEqualTo("INVOICE_PDF_GENERATION_REQUESTED");

        // Execute consumer
        invoicePdfOutboxConsumer.processSingleEvent(event);

        // Verify event updated to PROCESSED
        OutboxEventEntity updatedEvent = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updatedEvent.getStatus()).isEqualTo("PROCESSED");

        // Verify document is READY in invoice_documents
        InvoiceDocumentEntity doc = invoiceDocumentRepository
                .findByTenantIdAndInvoiceIdAndDocumentType(tenantAId, invoice.getId(), "INVOICE_PDF")
                .orElseThrow();
        assertThat(doc.getStatus()).isEqualTo(InvoicePdfStatus.READY);
        assertThat(doc.getFileSize()).isGreaterThan(1000);
    }
}
