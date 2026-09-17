package com.textile.erp.customer;

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
import com.textile.erp.auth.service.JwtService;
import com.textile.erp.customer.dto.BillingAddressDto;
import com.textile.erp.customer.dto.CreateCustomerRequest;
import com.textile.erp.customer.dto.CreateShippingPartyRequest;
import com.textile.erp.customer.dto.CustomerResponse;
import com.textile.erp.customer.dto.ShippingPartyResponse;
import com.textile.erp.customer.dto.UpdateShippingPartyRequest;
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
class ShippingPartyIntegrationTest {

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

    private UUID customerAId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        long ts = System.currentTimeMillis();

        TenantResponseDto tenantA = tenantService.createTenant(TenantRequestDto.builder()
                .name("Shipping Tenant A " + ts)
                .slug("ship-tenant-a-" + ts)
                .build());
        tenantAId = tenantA.getId();

        TenantResponseDto tenantB = tenantService.createTenant(TenantRequestDto.builder()
                .name("Shipping Tenant B " + ts)
                .slug("ship-tenant-b-" + ts)
                .build());
        tenantBId = tenantB.getId();

        User adminA = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantAId)
                .email("admin." + ts + "@shiptenanta.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("John")
                .lastName("Doe")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminA.getId(), RoleName.TENANT_ADMIN);

        User adminB = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantBId)
                .email("admin." + ts + "@shiptenantb.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Jane")
                .lastName("Smith")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminB.getId(), RoleName.TENANT_ADMIN);

        tenantAdminAToken = jwtService.generateAccessToken(
                adminA.getId(), tenantAId, adminA.getEmail(), Set.of(RoleName.TENANT_ADMIN));

        tenantAdminBToken = jwtService.generateAccessToken(
                adminB.getId(), tenantBId, adminB.getEmail(), Set.of(RoleName.TENANT_ADMIN));

        CreateCustomerRequest createCustomer = CreateCustomerRequest.builder()
                .customerCode("SHIP-CUST-1")
                .name("Kiran Garments Ltd")
                .gstin("24AAACK1234F1Z1")
                .pan("AAACK1234F")
                .phone("9988776655")
                .email("info@kirangarments.com")
                .billingAddress(BillingAddressDto.builder()
                        .addressLine1("Plot 45, GIDC Pandesara")
                        .city("Surat")
                        .state("Gujarat")
                        .pincode("394221")
                        .country("India")
                        .build())
                .paymentTermsDays(30)
                .creditLimit(new BigDecimal("200000.00"))
                .sameAsBilling(false)
                .build();

        String res = mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCustomer)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse customerResponse = objectMapper.readValue(res, CustomerResponse.class);
        customerAId = customerResponse.getId();
    }

    private CreateShippingPartyRequest sampleShippingPartyRequest(String partyName) {
        return CreateShippingPartyRequest.builder()
                .name(partyName)
                .gstin("24AAACK1234F1Z1")
                .pan("AAACK1234F")
                .addressLine1("Godown 3, Bhiwandi Road")
                .addressLine2("Near Toll Plaza")
                .city("Thane")
                .state("Maharashtra")
                .pincode("421302")
                .country("India")
                .sameAsBilling(false)
                .build();
    }

    @Test
    @DisplayName("Should create, retrieve, update, and delete shipping party")
    void shouldPerformShippingPartyLifecycle() throws Exception {
        // 1. Create shipping party
        CreateShippingPartyRequest request = sampleShippingPartyRequest("Thane Central Godown");

        String createJson = mockMvc.perform(post("/api/v1/customers/" + customerAId + "/shipping-parties")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Thane Central Godown"))
                .andExpect(jsonPath("$.city").value("Thane"))
                .andExpect(jsonPath("$.state").value("Maharashtra"))
                .andReturn().getResponse().getContentAsString();

        ShippingPartyResponse party = objectMapper.readValue(createJson, ShippingPartyResponse.class);
        UUID partyId = party.getId();

        // 2. List shipping parties for customer
        mockMvc.perform(get("/api/v1/customers/" + customerAId + "/shipping-parties")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Thane Central Godown"));

        // 3. Get shipping party by ID
        mockMvc.perform(get("/api/v1/customers/" + customerAId + "/shipping-parties/" + partyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(partyId.toString()))
                .andExpect(jsonPath("$.city").value("Thane"));

        // 4. Update shipping party
        UpdateShippingPartyRequest updateReq = UpdateShippingPartyRequest.builder()
                .name("Thane Logistics Hub - Unit 2")
                .gstin("27AAACK1234F1Z9")
                .pan("AAACK1234F")
                .addressLine1("Godown 3B, Bhiwandi Road")
                .city("Thane")
                .state("Maharashtra")
                .pincode("421302")
                .country("India")
                .sameAsBilling(false)
                .build();

        mockMvc.perform(put("/api/v1/customers/" + customerAId + "/shipping-parties/" + partyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Thane Logistics Hub - Unit 2"))
                .andExpect(jsonPath("$.addressLine1").value("Godown 3B, Bhiwandi Road"));

        // 5. Delete shipping party
        mockMvc.perform(delete("/api/v1/customers/" + customerAId + "/shipping-parties/" + partyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken))
                .andExpect(status().isNoContent());

        // 6. Verify 404 after deletion
        mockMvc.perform(get("/api/v1/customers/" + customerAId + "/shipping-parties/" + partyId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should prevent Tenant B from accessing or manipulating Tenant A shipping parties")
    void shouldEnforceIsolationForShippingParties() throws Exception {
        CreateShippingPartyRequest request = sampleShippingPartyRequest("Private Warehouse A");

        String createJson = mockMvc.perform(post("/api/v1/customers/" + customerAId + "/shipping-parties")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ShippingPartyResponse party = objectMapper.readValue(createJson, ShippingPartyResponse.class);

        // Tenant B trying to fetch party -> 404
        mockMvc.perform(get("/api/v1/customers/" + customerAId + "/shipping-parties/" + party.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminBToken))
                .andExpect(status().isNotFound());

        // Tenant B trying to delete party -> 404
        mockMvc.perform(delete("/api/v1/customers/" + customerAId + "/shipping-parties/" + party.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminBToken))
                .andExpect(status().isNotFound());
    }
}
