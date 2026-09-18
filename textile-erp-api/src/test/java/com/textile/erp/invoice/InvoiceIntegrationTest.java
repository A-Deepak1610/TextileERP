package com.textile.erp.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.textile.erp.agent.dto.AgentResponse;
import com.textile.erp.agent.dto.CreateAgentRequest;
import com.textile.erp.agent.entity.CommissionType;
import com.textile.erp.auth.service.JwtService;
import com.textile.erp.customer.dto.BillingAddressDto;
import com.textile.erp.customer.dto.CreateCustomerRequest;
import com.textile.erp.customer.dto.CustomerResponse;
import com.textile.erp.invoice.dto.CancelInvoiceRequest;
import com.textile.erp.invoice.dto.CreateInvoiceRequest;
import com.textile.erp.invoice.dto.InvoiceBaleRequest;
import com.textile.erp.invoice.dto.InvoiceItemRequest;
import com.textile.erp.invoice.dto.InvoiceResponse;
import com.textile.erp.invoice.dto.UpdateInvoiceRequest;
import com.textile.erp.invoice.entity.CustomerLedgerEntry;
import com.textile.erp.invoice.entity.InvoiceStatus;
import com.textile.erp.invoice.entity.LedgerEntryType;
import com.textile.erp.invoice.entity.PaymentMode;
import com.textile.erp.invoice.repository.CustomerLedgerEntryRepository;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class InvoiceIntegrationTest {

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
    private CustomerLedgerEntryRepository customerLedgerEntryRepository;

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    private UUID tenantAId;
    private UUID tenantBId;

    private String tokenA;
    private String tokenB;

    private UUID customerAId;
    private UUID productAId;
    private UUID agentAId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        long ts = System.currentTimeMillis();

        TenantResponseDto tenantA = tenantService.createTenant(TenantRequestDto.builder()
                .name("Murugan Textiles " + ts)
                .slug("murugan-tex-" + ts)
                .build());
        tenantAId = tenantA.getId();

        TenantResponseDto tenantB = tenantService.createTenant(TenantRequestDto.builder()
                .name("Other Textiles " + ts)
                .slug("other-tex-" + ts)
                .build());
        tenantBId = tenantB.getId();

        User adminA = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantAId)
                .email("admin." + ts + "@murugantex.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Murugan")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminA.getId(), RoleName.TENANT_ADMIN);

        User adminB = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantBId)
                .email("admin." + ts + "@othertex.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Other")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminB.getId(), RoleName.TENANT_ADMIN);

        tokenA = jwtService.generateAccessToken(adminA.getId(), tenantAId, adminA.getEmail(), Set.of(RoleName.TENANT_ADMIN));
        tokenB = jwtService.generateAccessToken(adminB.getId(), tenantBId, adminB.getEmail(), Set.of(RoleName.TENANT_ADMIN));

        // Create Customer for Tenant A
        CreateCustomerRequest custReq = CreateCustomerRequest.builder()
                .customerCode("CUST-MT-" + ts)
                .name("Sri Lakshmi Traders")
                .gstin("33AABCS1429B1ZB")
                .pan("AABCS1429B")
                .phone("9443312345")
                .email("billing@srilakshmi.com")
                .billingAddress(BillingAddressDto.builder()
                        .addressLine1("14 Karur Byepass Road")
                        .city("Erode")
                        .state("Tamil Nadu")
                        .pincode("638002")
                        .country("India")
                        .build())
                .paymentTermsDays(30)
                .creditLimit(new BigDecimal("500000.00"))
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

        // Create Product for Tenant A
        CreateProductRequest prodReq = CreateProductRequest.builder()
                .productCode("PROD-COTTON-" + ts)
                .productName("Cotton Grey Fabric 40s")
                .hsnCode("5208")
                .fabricType("Cotton")
                .color("Grey")
                .gsm(120)
                .unit("METER")
                .defaultRatePerUnit(new BigDecimal("45.50"))
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

        // Create Agent for Tenant A
        CreateAgentRequest agentReq = CreateAgentRequest.builder()
                .agentName("Suresh Kumar Agent")
                .mobile("9842112345")
                .defaultCommissionType(CommissionType.PERCENTAGE)
                .defaultCommissionValue(new BigDecimal("2.00"))
                .build();

        String agentRes = mockMvc.perform(post("/api/v1/agents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        AgentResponse agent = objectMapper.readValue(agentRes, AgentResponse.class);
        agentAId = agent.getId();
    }

    private CreateInvoiceRequest buildSampleInvoiceRequest() {
        InvoiceBaleRequest bale1 = InvoiceBaleRequest.builder()
                .baleNumber("B1")
                .meters(new BigDecimal("1000.000"))
                .pieceCount(10)
                .build();

        InvoiceItemRequest item1 = InvoiceItemRequest.builder()
                .productId(productAId)
                .description("Cotton Grey Fabric 40s - Premium Weave")
                .hsnCode("5208")
                .meters(new BigDecimal("1000.000"))
                .foldingLessPercent(new BigDecimal("2.000"))
                .ratePerMeter(new BigDecimal("45.50"))
                .bales(List.of(bale1))
                .build();

        return CreateInvoiceRequest.builder()
                .customerId(customerAId)
                .sameAsBilling(true)
                .agentId(agentAId)
                .commissionType(CommissionType.PERCENTAGE)
                .commissionValue(new BigDecimal("2.00"))
                .invoiceDate(LocalDate.now())
                .creditDays(30)
                .paymentMode(PaymentMode.CREDIT)
                .paymentTermsNote("Murugan Tex Sample Order")
                .items(List.of(item1))
                .build();
    }

    @Test
    @DisplayName("Should create draft invoice and verify textile calculations & snapshots")
    void shouldCreateDraftInvoice() throws Exception {
        CreateInvoiceRequest request = buildSampleInvoiceRequest();

        mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items[0].meters").value(1000.0))
                .andExpect(jsonPath("$.items[0].foldingLessMeters").value(20.0))
                .andExpect(jsonPath("$.items[0].totalMeters").value(980.0))
                .andExpect(jsonPath("$.items[0].taxableAmount").value(44590.0))
                .andExpect(jsonPath("$.subtotal").value(44590.0))
                .andExpect(jsonPath("$.grandTotal").isNumber())
                .andExpect(jsonPath("$.amountInWords").isNotEmpty())
                .andExpect(jsonPath("$.billingSnapshot.customerName").value("Sri Lakshmi Traders"))
                .andExpect(jsonPath("$.agentNameSnapshot").value("Suresh Kumar Agent"));
    }

    @Test
    @DisplayName("Should update draft invoice and recalculate totals")
    void shouldUpdateDraftInvoice() throws Exception {
        CreateInvoiceRequest createReq = buildSampleInvoiceRequest();
        String createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        InvoiceResponse invoice = objectMapper.readValue(createRes, InvoiceResponse.class);

        // Update with 500 meters instead of 1000 meters
        UpdateInvoiceRequest updateReq = UpdateInvoiceRequest.builder()
                .sameAsBilling(true)
                .invoiceDate(LocalDate.now())
                .creditDays(45)
                .items(List.of(InvoiceItemRequest.builder()
                        .productId(productAId)
                        .description("Updated Fabric Description")
                        .meters(new BigDecimal("500.000"))
                        .foldingLessPercent(new BigDecimal("0.000"))
                        .ratePerMeter(new BigDecimal("50.00"))
                        .build()))
                .build();

        mockMvc.perform(put("/api/v1/invoices/" + invoice.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.creditDays").value(45))
                .andExpect(jsonPath("$.subtotal").value(25000.0));
    }

    @Test
    @DisplayName("Should issue invoice, assign official FY sequence number, and create ledger debit entry")
    void shouldIssueInvoiceSuccessfully() throws Exception {
        CreateInvoiceRequest createReq = buildSampleInvoiceRequest();
        String createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        InvoiceResponse invoice = objectMapper.readValue(createRes, InvoiceResponse.class);

        String issueRes = mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/issue")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.invoiceNumber").value(org.hamcrest.Matchers.startsWith("SAL/")))
                .andReturn().getResponse().getContentAsString();

        InvoiceResponse issued = objectMapper.readValue(issueRes, InvoiceResponse.class);
        assertThat(issued.getIssuedAt()).isNotNull();
        assertThat(issued.getPdfUrl()).isNotNull();

        // Verify Customer Ledger DEBIT entry
        List<CustomerLedgerEntry> entries = customerLedgerEntryRepository
                .findByTenantIdAndCustomerIdOrderByEntryDateAscCreatedAtAsc(tenantAId, customerAId);

        assertThat(entries).isNotEmpty();
        CustomerLedgerEntry lastEntry = entries.get(entries.size() - 1);
        assertThat(lastEntry.getEntryType()).isEqualTo(LedgerEntryType.DEBIT);
        assertThat(lastEntry.getAmount()).isEqualByComparingTo(issued.getGrandTotal());
        assertThat(lastEntry.getRunningBalance()).isEqualByComparingTo(issued.getGrandTotal());
    }

    @Test
    @DisplayName("Should enforce immutability: Cannot update or delete an ISSUED invoice")
    void shouldEnforceImmutabilityOnIssuedInvoice() throws Exception {
        CreateInvoiceRequest createReq = buildSampleInvoiceRequest();
        String createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();
        InvoiceResponse invoice = objectMapper.readValue(createRes, InvoiceResponse.class);

        // Issue it
        mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/issue")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Attempt to update -> 400 Bad Request
        UpdateInvoiceRequest updateReq = UpdateInvoiceRequest.builder()
                .items(List.of(InvoiceItemRequest.builder()
                        .meters(new BigDecimal("100.000"))
                        .ratePerMeter(new BigDecimal("10.00"))
                        .build()))
                .build();

        mockMvc.perform(put("/api/v1/invoices/" + invoice.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isConflict());

        // Attempt to delete -> 409 Conflict
        mockMvc.perform(delete("/api/v1/invoices/" + invoice.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should cancel issued invoice and record ledger credit reversal")
    void shouldCancelIssuedInvoice() throws Exception {
        CreateInvoiceRequest createReq = buildSampleInvoiceRequest();
        String createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();
        InvoiceResponse invoice = objectMapper.readValue(createRes, InvoiceResponse.class);

        // Issue it
        mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/issue")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Cancel it
        CancelInvoiceRequest cancelReq = CancelInvoiceRequest.builder()
                .cancellationReason("Customer rejected fabric due to color mismatch")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Customer rejected fabric due to color mismatch"));

        // Verify Customer Ledger has CREDIT reversal entry with running balance back to 0.00
        List<CustomerLedgerEntry> entries = customerLedgerEntryRepository
                .findByTenantIdAndCustomerIdOrderByEntryDateAscCreatedAtAsc(tenantAId, customerAId);

        assertThat(entries.size()).isGreaterThanOrEqualTo(2);
        CustomerLedgerEntry lastEntry = entries.get(entries.size() - 1);
        assertThat(lastEntry.getEntryType()).isEqualTo(LedgerEntryType.CREDIT);
        assertThat(lastEntry.getRunningBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Should enforce tenant isolation across tenants A and B")
    void shouldEnforceTenantIsolation() throws Exception {
        CreateInvoiceRequest createReq = buildSampleInvoiceRequest();
        String createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        InvoiceResponse invoice = objectMapper.readValue(createRes, InvoiceResponse.class);

        // Tenant B cannot GET Tenant A's invoice -> 404
        mockMvc.perform(get("/api/v1/invoices/" + invoice.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Tenant B cannot ISSUE Tenant A's invoice -> 404
        mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/issue")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Tenant B cannot DELETE Tenant A's invoice -> 404
        mockMvc.perform(delete("/api/v1/invoices/" + invoice.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should delete DRAFT invoice successfully")
    void shouldDeleteDraftInvoice() throws Exception {
        CreateInvoiceRequest createReq = buildSampleInvoiceRequest();
        String createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        InvoiceResponse invoice = objectMapper.readValue(createRes, InvoiceResponse.class);

        mockMvc.perform(delete("/api/v1/invoices/" + invoice.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/invoices/" + invoice.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
