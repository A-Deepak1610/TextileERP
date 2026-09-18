package com.textile.erp.product;

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
import com.textile.erp.product.dto.CreateProductRequest;
import com.textile.erp.product.dto.ProductResponse;
import com.textile.erp.product.dto.UpdateProductRequest;
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
class ProductIntegrationTest {

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
                .name("Product Tenant A " + ts)
                .slug("prod-tenant-a-" + ts)
                .build());
        tenantAId = tenantA.getId();

        TenantResponseDto tenantB = tenantService.createTenant(TenantRequestDto.builder()
                .name("Product Tenant B " + ts)
                .slug("prod-tenant-b-" + ts)
                .build());
        tenantBId = tenantB.getId();

        User adminA = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantAId)
                .email("admin." + ts + "@prodtenanta.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Product")
                .lastName("AdminA")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminA.getId(), RoleName.TENANT_ADMIN);

        User adminB = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantBId)
                .email("admin." + ts + "@prodtenantb.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Product")
                .lastName("AdminB")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminB.getId(), RoleName.TENANT_ADMIN);

        tenantAdminAToken = jwtService.generateAccessToken(
                adminA.getId(), tenantAId, adminA.getEmail(), Set.of(RoleName.TENANT_ADMIN));

        tenantAdminBToken = jwtService.generateAccessToken(
                adminB.getId(), tenantBId, adminB.getEmail(), Set.of(RoleName.TENANT_ADMIN));
    }

    private CreateProductRequest sampleProductRequest(String code, String name) {
        return CreateProductRequest.builder()
                .productCode(code)
                .productName(name)
                .description("High grade cotton fabric")
                .hsnCode("5208")
                .fabricType("100% Cotton Poplin")
                .color("Pure White")
                .gsm(120)
                .unit("METER")
                .defaultRatePerUnit(new BigDecimal("38.50"))
                .gstRate(new BigDecimal("5.00"))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should create product and retrieve by ID")
    void shouldCreateAndGetProduct() throws Exception {
        CreateProductRequest request = sampleProductRequest("TEX-COT-01", "Cotton Cambric 60s");

        String res = mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.productCode").value("TEX-COT-01"))
                .andExpect(jsonPath("$.productName").value("Cotton Cambric 60s"))
                .andExpect(jsonPath("$.hsnCode").value("5208"))
                .andExpect(jsonPath("$.defaultRatePerUnit").value(38.50))
                .andExpect(jsonPath("$.unit").value("METER"))
                .andReturn().getResponse().getContentAsString();

        ProductResponse response = objectMapper.readValue(res, ProductResponse.class);

        mockMvc.perform(get("/api/v1/products/" + response.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Cotton Cambric 60s"));
    }

    @Test
    @DisplayName("Should reject duplicate product code in same tenant with 409 Conflict")
    void shouldRejectDuplicateProductCode() throws Exception {
        CreateProductRequest request = sampleProductRequest("DUP-CODE-01", "Item One");

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("Should allow same product code in different tenants")
    void shouldAllowSameCodeInDifferentTenants() throws Exception {
        CreateProductRequest request = sampleProductRequest("SHARED-PROD-01", "Shared Product");

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should list and search products with pagination and filters")
    void shouldListAndSearchProducts() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleProductRequest("PRD-A1", "Rayon Twill"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleProductRequest("PRD-A2", "Silk Crepe"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .param("query", "rayon")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("Rayon Twill"));
    }

    @Test
    @DisplayName("Should update product and toggle status")
    void shouldUpdateAndToggleProduct() throws Exception {
        String res = mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleProductRequest("PRD-MOD-01", "Original Linen"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ProductResponse created = objectMapper.readValue(res, ProductResponse.class);

        UpdateProductRequest updateReq = UpdateProductRequest.builder()
                .productCode("PRD-MOD-01")
                .productName("Pure European Linen")
                .defaultRatePerUnit(new BigDecimal("95.00"))
                .active(true)
                .build();

        mockMvc.perform(put("/api/v1/products/" + created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("Pure European Linen"))
                .andExpect(jsonPath("$.defaultRatePerUnit").value(95.00));

        mockMvc.perform(patch("/api/v1/products/" + created.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("Should enforce cross-tenant isolation on products")
    void shouldEnforceCrossTenantIsolation() throws Exception {
        String res = mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleProductRequest("ISO-PRD-1", "Tenant A Product"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ProductResponse created = objectMapper.readValue(res, ProductResponse.class);

        // Tenant B attempts GET -> 404
        mockMvc.perform(get("/api/v1/products/" + created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminBToken))
                .andExpect(status().isNotFound());

        // Tenant B attempts DELETE -> 404
        mockMvc.perform(delete("/api/v1/products/" + created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminBToken))
                .andExpect(status().isNotFound());
    }
}
