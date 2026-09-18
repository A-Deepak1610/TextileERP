package com.textile.erp.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.textile.erp.auth.service.JwtService;
import com.textile.erp.customer.dto.BillingAddressDto;
import com.textile.erp.customer.dto.CreateCustomerRequest;
import com.textile.erp.customer.dto.CustomerResponse;
import com.textile.erp.invoice.dto.CreateInvoiceRequest;
import com.textile.erp.invoice.dto.InvoiceBaleRequest;
import com.textile.erp.invoice.dto.InvoiceItemRequest;
import com.textile.erp.invoice.dto.InvoiceResponse;
import com.textile.erp.invoice.dto.RecordPaymentRequest;
import com.textile.erp.invoice.entity.CustomerLedgerEntry;
import com.textile.erp.invoice.entity.LedgerEntryType;
import com.textile.erp.invoice.entity.PaymentMode;
import com.textile.erp.invoice.entity.PaymentStatus;
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
class InvoicePaymentAndLedgerIntegrationTest {

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

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        long ts = System.currentTimeMillis();

        TenantResponseDto tenantA = tenantService.createTenant(TenantRequestDto.builder()
                .name("Payment Ledger Tex A " + ts)
                .slug("pay-ledg-a-" + ts)
                .build());
        tenantAId = tenantA.getId();

        TenantResponseDto tenantB = tenantService.createTenant(TenantRequestDto.builder()
                .name("Payment Ledger Tex B " + ts)
                .slug("pay-ledg-b-" + ts)
                .build());
        tenantBId = tenantB.getId();

        User adminA = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantAId)
                .email("admin." + ts + "@payledga.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Pay")
                .lastName("AdminA")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminA.getId(), RoleName.TENANT_ADMIN);

        User adminB = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantBId)
                .email("admin." + ts + "@payledgb.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Pay")
                .lastName("AdminB")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminB.getId(), RoleName.TENANT_ADMIN);

        tokenA = jwtService.generateAccessToken(adminA.getId(), tenantAId, adminA.getEmail(), Set.of(RoleName.TENANT_ADMIN));
        tokenB = jwtService.generateAccessToken(adminB.getId(), tenantBId, adminB.getEmail(), Set.of(RoleName.TENANT_ADMIN));

        // Create Customer
        CreateCustomerRequest custReq = CreateCustomerRequest.builder()
                .customerCode("CUST-PAY-" + ts)
                .name("Kavitha Sarees")
                .gstin("33AABCK1234F1Z9")
                .pan("AABCK1234F")
                .phone("9876543210")
                .email("kavitha@sarees.com")
                .billingAddress(BillingAddressDto.builder()
                        .addressLine1("10 Bazaar Street")
                        .city("Salem")
                        .state("Tamil Nadu")
                        .pincode("636001")
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
                .productCode("PROD-SILK-" + ts)
                .productName("Art Silk Yarn Dyed Fabric")
                .hsnCode("5407")
                .fabricType("Silk")
                .color("Maroon")
                .gsm(150)
                .unit("METER")
                .defaultRatePerUnit(new BigDecimal("100.00"))
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
                .description("Art Silk Yarn Dyed Fabric")
                .hsnCode("5407")
                .meters(new BigDecimal("500.000")) // 500 * 100 = 50,000.00 taxable
                .foldingLessPercent(BigDecimal.ZERO)
                .ratePerMeter(new BigDecimal("100.00"))
                .bales(List.of(InvoiceBaleRequest.builder()
                        .baleNumber("B-01")
                        .meters(new BigDecimal("500.000"))
                        .pieceCount(5)
                        .build()))
                .build();

        CreateInvoiceRequest createReq = CreateInvoiceRequest.builder()
                .customerId(customerAId)
                .sameAsBilling(true)
                .invoiceDate(LocalDate.now())
                .paymentMode(PaymentMode.NEFT)
                .creditDays(30)
                .issueNow(true) // Issue immediately!
                .items(List.of(item1))
                .build();

        // Subtotal = 50,000.00, Intrastate 5% GST = 2,500.00, Grand Total = 52,500.00
        String createRes = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.grandTotal").value(52500.0))
                .andExpect(jsonPath("$.dueAmount").value(52500.0))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(createRes, InvoiceResponse.class);
    }

    @Test
    @DisplayName("Should record partial payment and verify PARTIALLY_PAID status and ledger credit")
    void shouldRecordPartialPayment() throws Exception {
        InvoiceResponse invoice = createAndIssueSampleInvoice();

        // Record partial payment of Rs. 20,000.00 via NEFT
        RecordPaymentRequest paymentReq = RecordPaymentRequest.builder()
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("20000.00"))
                .paymentMode(PaymentMode.NEFT)
                .referenceNumber("NEFT-TXN-987654")
                .notes("Advance partial payment")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.totalPaidAmount").value(20000.00))
                .andExpect(jsonPath("$.dueAmount").value(32500.00));

        // Verify Customer Ledger credit entry
        List<CustomerLedgerEntry> ledger = customerLedgerEntryRepository
                .findByTenantIdAndCustomerIdOrderByEntryDateAscCreatedAtAsc(tenantAId, customerAId);

        assertThat(ledger).hasSize(2); // 1 Debit (52500) + 1 Credit (20000)
        CustomerLedgerEntry creditEntry = ledger.get(1);
        assertThat(creditEntry.getEntryType()).isEqualTo(LedgerEntryType.CREDIT);
        assertThat(creditEntry.getAmount()).isEqualByComparingTo("20000.00");
        assertThat(creditEntry.getRunningBalance()).isEqualByComparingTo("32500.00");
    }

    @Test
    @DisplayName("Should complete payment in full and verify PAID status and 0.00 ledger balance")
    void shouldRecordFullPayment() throws Exception {
        InvoiceResponse invoice = createAndIssueSampleInvoice();

        // 1. Partial payment: 30,000.00
        RecordPaymentRequest pay1 = RecordPaymentRequest.builder()
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("30000.00"))
                .paymentMode(PaymentMode.RTGS)
                .referenceNumber("RTGS-TXN-001")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pay1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PARTIALLY_PAID"));

        // 2. Final settlement payment: 22,500.00
        RecordPaymentRequest pay2 = RecordPaymentRequest.builder()
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("22500.00"))
                .paymentMode(PaymentMode.UPI)
                .referenceNumber("UPI-TXN-002")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pay2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.totalPaidAmount").value(52500.00))
                .andExpect(jsonPath("$.dueAmount").value(0.00));

        // Check balance via Customer Ledger Balance Endpoint
        mockMvc.perform(get("/api/v1/customers/" + customerAId + "/ledger/balance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outstandingBalance").value(0.0));
    }

    @Test
    @DisplayName("Should reject payment exceeding due amount with 400 Bad Request")
    void shouldRejectOverpayment() throws Exception {
        InvoiceResponse invoice = createAndIssueSampleInvoice();

        RecordPaymentRequest overpay = RecordPaymentRequest.builder()
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("60000.00")) // due is 52500.00
                .paymentMode(PaymentMode.CHEQUE)
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overpay)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject payment on DRAFT invoice")
    void shouldRejectPaymentOnDraftInvoice() throws Exception {
        InvoiceItemRequest item1 = InvoiceItemRequest.builder()
                .productId(productAId)
                .meters(new BigDecimal("100.000"))
                .ratePerMeter(new BigDecimal("50.00"))
                .build();

        CreateInvoiceRequest draftReq = CreateInvoiceRequest.builder()
                .customerId(customerAId)
                .sameAsBilling(true)
                .issueNow(false) // DRAFT!
                .items(List.of(item1))
                .build();

        String draftRes = mockMvc.perform(post("/api/v1/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        InvoiceResponse draft = objectMapper.readValue(draftRes, InvoiceResponse.class);

        RecordPaymentRequest payReq = RecordPaymentRequest.builder()
                .amount(new BigDecimal("1000.00"))
                .paymentMode(PaymentMode.CASH)
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + draft.getId() + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Should retrieve complete customer ledger statement and payment history")
    void shouldRetrieveCustomerLedgerStatement() throws Exception {
        InvoiceResponse invoice = createAndIssueSampleInvoice();

        // Add payment
        RecordPaymentRequest payReq = RecordPaymentRequest.builder()
                .paymentDate(LocalDate.now())
                .amount(new BigDecimal("10000.00"))
                .paymentMode(PaymentMode.BANK_TRANSFER)
                .referenceNumber("BT-9911")
                .build();

        mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk());

        // 1. Check customer ledger list
        mockMvc.perform(get("/api/v1/customers/" + customerAId + "/ledger")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].entryType").value("DEBIT"))
                .andExpect(jsonPath("$[0].amount").value(52500.0))
                .andExpect(jsonPath("$[1].entryType").value("CREDIT"))
                .andExpect(jsonPath("$[1].amount").value(10000.0))
                .andExpect(jsonPath("$[1].runningBalance").value(42500.0));

        // 2. Check invoice payments endpoint
        mockMvc.perform(get("/api/v1/invoices/" + invoice.getId() + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amount").value(10000.00))
                .andExpect(jsonPath("$[0].paymentMode").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$[0].referenceNumber").value("BT-9911"));
    }

    @Test
    @DisplayName("Should enforce tenant isolation on payments and customer ledger")
    void shouldEnforceTenantIsolationOnPayments() throws Exception {
        InvoiceResponse invoice = createAndIssueSampleInvoice();

        RecordPaymentRequest payReq = RecordPaymentRequest.builder()
                .amount(new BigDecimal("5000.00"))
                .paymentMode(PaymentMode.CASH)
                .build();

        // Tenant B cannot record payment on Tenant A's invoice -> 404
        mockMvc.perform(post("/api/v1/invoices/" + invoice.getId() + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isNotFound());

        // Tenant B cannot access Tenant A's customer ledger -> 404
        mockMvc.perform(get("/api/v1/customers/" + customerAId + "/ledger")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }
}
