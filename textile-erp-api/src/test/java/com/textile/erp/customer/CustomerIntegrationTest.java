package com.textile.erp.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.textile.erp.auth.service.JwtService;
import com.textile.erp.customer.dto.BillingAddressDto;
import com.textile.erp.customer.dto.CreateCustomerRequest;
import com.textile.erp.customer.dto.CustomerResponse;
import com.textile.erp.customer.dto.UpdateCustomerRequest;
import com.textile.erp.customer.dto.UpdateCustomerStatusRequest;
import com.textile.erp.customer.entity.CustomerStatus;
import com.textile.erp.user.dto.TenantRequestDto;
import com.textile.erp.user.dto.TenantResponseDto;
import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.User;
import com.textile.erp.user.entity.UserStatus;
import com.textile.erp.user.repository.UserRepository;
import com.textile.erp.user.service.TenantService;
import com.textile.erp.user.service.UserService;
import java.math.BigDecimal;
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
class CustomerIntegrationTest {

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

    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    private UUID tenantAId;
    private UUID tenantBId;

    private String tenantAdminAToken;
    private String tenantAdminBToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        long ts = System.currentTimeMillis();

        TenantResponseDto tenantA = tenantService.createTenant(TenantRequestDto.builder()
                .name("Textile Tenant A " + ts)
                .slug("textile-tenant-a-" + ts)
                .build());
        tenantAId = tenantA.getId();

        TenantResponseDto tenantB = tenantService.createTenant(TenantRequestDto.builder()
                .name("Textile Tenant B " + ts)
                .slug("textile-tenant-b-" + ts)
                .build());
        tenantBId = tenantB.getId();

        User adminA = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantAId)
                .email("admin." + ts + "@tenanta.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Alice")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminA.getId(), RoleName.TENANT_ADMIN);

        User adminB = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantBId)
                .email("admin." + ts + "@tenantb.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Bob")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminB.getId(), RoleName.TENANT_ADMIN);

        tenantAdminAToken = jwtService.generateAccessToken(
                adminA.getId(), tenantAId, adminA.getEmail(), Set.of(RoleName.TENANT_ADMIN));

        tenantAdminBToken = jwtService.generateAccessToken(
                adminB.getId(), tenantBId, adminB.getEmail(), Set.of(RoleName.TENANT_ADMIN));
    }

    private BillingAddressDto sampleAddress() {
        return BillingAddressDto.builder()
                .addressLine1("12 Ring Road")
                .addressLine2("Near Textile Market")
                .city("Surat")
                .state("Gujarat")
                .pincode("395002")
                .country("India")
                .build();
    }

    private CreateCustomerRequest sampleCustomerRequest(String code, String name, boolean sameAsBilling) {
        return CreateCustomerRequest.builder()
                .customerCode(code)
                .name(name)
                .gstin("24AAACH7409R1ZZ")
                .pan("AAACH7409R")
                .phone("9876543210")
                .email("billing@" + code.toLowerCase() + ".com")
                .billingAddress(sampleAddress())
                .paymentTermsDays(30)
                .creditLimit(new BigDecimal("500000.00"))
                .sameAsBilling(sameAsBilling)
                .build();
    }

    @Test
    @DisplayName("Should create customer with sameAsBilling=true and verify auto-created shipping party")
    void shouldCreateCustomerWithSameAsBilling() throws Exception {
        CreateCustomerRequest request = sampleCustomerRequest("CUST-001", "Reliance Textiles Ltd", true);

        String responseJson = mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.customerCode").value("CUST-001"))
                .andExpect(jsonPath("$.name").value("Reliance Textiles Ltd"))
                .andExpect(jsonPath("$.gstin").value("24AAACH7409R1ZZ"))
                .andExpect(jsonPath("$.pan").value("AAACH7409R"))
                .andExpect(jsonPath("$.billingAddress.city").value("Surat"))
                .andExpect(jsonPath("$.shippingParties").isArray())
                .andExpect(jsonPath("$.shippingParties.length()").value(1))
                .andExpect(jsonPath("$.shippingParties[0].name").value("Reliance Textiles Ltd"))
                .andExpect(jsonPath("$.shippingParties[0].sameAsBilling").value(true))
                .andExpect(jsonPath("$.shippingParties[0].city").value("Surat"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        CustomerResponse response = objectMapper.readValue(responseJson, CustomerResponse.class);
        assertThat(response.getId()).isNotNull();
    }

    @Test
    @DisplayName("Should get paginated customer list with search and status filter")
    void shouldGetPaginatedCustomerListWithSearchAndFilter() throws Exception {
        // Create 2 customers in Tenant A
        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCustomerRequest("CUST-A1", "Arvind Mills", false))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCustomerRequest("CUST-A2", "Raymond Fabrics", false))))
                .andExpect(status().isCreated());

        // Search with query 'arvind'
        mockMvc.perform(get("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .param("query", "arvind")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Arvind Mills"));

        // Filter by status ACTIVE
        mockMvc.perform(get("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("Should update customer profile")
    void shouldUpdateCustomerProfile() throws Exception {
        String createJson = mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCustomerRequest("CUST-UP1", "Bombay Dyeing", false))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readValue(createJson, CustomerResponse.class);

        UpdateCustomerRequest updateReq = UpdateCustomerRequest.builder()
                .customerCode("CUST-UP1-MOD")
                .name("Bombay Dyeing & Mfg Co")
                .gstin("27AAACB2154P1ZU")
                .pan("AAACB2154P")
                .phone("9123456780")
                .email("accounts@bombaydyeing.com")
                .billingAddress(BillingAddressDto.builder()
                        .addressLine1("Neville House, Ballard Estate")
                        .city("Mumbai")
                        .state("Maharashtra")
                        .pincode("400001")
                        .country("India")
                        .build())
                .paymentTermsDays(45)
                .creditLimit(new BigDecimal("1500000.00"))
                .build();

        mockMvc.perform(put("/api/v1/customers/" + created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerCode").value("CUST-UP1-MOD"))
                .andExpect(jsonPath("$.name").value("Bombay Dyeing & Mfg Co"))
                .andExpect(jsonPath("$.billingAddress.city").value("Mumbai"))
                .andExpect(jsonPath("$.paymentTermsDays").value(45))
                .andExpect(jsonPath("$.creditLimit").value(1500000.00));
    }

    @Test
    @DisplayName("Should update customer status via PATCH")
    void shouldUpdateCustomerStatus() throws Exception {
        String createJson = mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCustomerRequest("CUST-ST1", "Vardhman Textiles", false))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readValue(createJson, CustomerResponse.class);

        UpdateCustomerStatusRequest patchReq = UpdateCustomerStatusRequest.builder()
                .status(CustomerStatus.INACTIVE)
                .build();

        mockMvc.perform(patch("/api/v1/customers/" + created.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("Should delete customer")
    void shouldDeleteCustomer() throws Exception {
        String createJson = mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCustomerRequest("CUST-DEL1", "Welspun Living", true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readValue(createJson, CustomerResponse.class);

        mockMvc.perform(delete("/api/v1/customers/" + created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/customers/" + created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reject duplicate customer_code in same tenant with 409 Conflict")
    void shouldRejectDuplicateCustomerCodeInSameTenant() throws Exception {
        CreateCustomerRequest request = sampleCustomerRequest("CODE-DUP", "First Company", false);

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("Should allow identical customer_code in different tenants (multi-tenant isolation)")
    void shouldAllowSameCustomerCodeInDifferentTenants() throws Exception {
        CreateCustomerRequest request = sampleCustomerRequest("SHARED-CODE", "Unique Across Tenant", false);

        // Tenant A creates
        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Tenant B creates same code -> SUCCESS
        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should reject invalid GSTIN, PAN, and Pincode format with 400 Bad Request")
    void shouldRejectInvalidFormats() throws Exception {
        CreateCustomerRequest invalidGstin = sampleCustomerRequest("CUST-INV1", "Bad GSTIN Corp", false);
        invalidGstin.setGstin("INVALID_GSTIN_123");

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidGstin)))
                .andExpect(status().isBadRequest());

        CreateCustomerRequest invalidPan = sampleCustomerRequest("CUST-INV2", "Bad PAN Corp", false);
        invalidPan.setPan("12345ABCDE"); // invalid PAN

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPan)))
                .andExpect(status().isBadRequest());

        CreateCustomerRequest invalidPin = sampleCustomerRequest("CUST-INV3", "Bad Pincode Corp", false);
        invalidPin.getBillingAddress().setPincode("01234"); // invalid Indian pincode

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should enforce cross-tenant isolation: Tenant B cannot read, update, or delete Tenant A's customer")
    void shouldEnforceCrossTenantIsolation() throws Exception {
        String createJson = mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCustomerRequest("ISO-001", "Tenant A Only", false))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readValue(createJson, CustomerResponse.class);

        // Tenant B attempts GET -> 404
        mockMvc.perform(get("/api/v1/customers/" + created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminBToken))
                .andExpect(status().isNotFound());

        // Tenant B attempts PUT -> 404
        UpdateCustomerRequest updateReq = UpdateCustomerRequest.builder()
                .customerCode("ISO-001")
                .name("Tenant B Trying to Hijack")
                .phone("9876543210")
                .billingAddress(sampleAddress())
                .build();

        mockMvc.perform(put("/api/v1/customers/" + created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Tenant B attempts DELETE -> 404
        mockMvc.perform(delete("/api/v1/customers/" + created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reject unauthenticated requests with 401")
    void shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleCustomerRequest("UNAUTH", "No Auth", false))))
                .andExpect(status().isUnauthorized());
    }
}
