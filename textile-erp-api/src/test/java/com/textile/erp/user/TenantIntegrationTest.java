package com.textile.erp.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.textile.erp.auth.service.JwtService;
import com.textile.erp.user.dto.TenantRequestDto;
import com.textile.erp.user.dto.TenantResponseDto;
import com.textile.erp.user.dto.UpdateTenantRequest;
import com.textile.erp.user.dto.UpdateTenantStatusRequest;
import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.TenantStatus;
import com.textile.erp.user.entity.User;
import com.textile.erp.user.entity.UserStatus;
import com.textile.erp.user.repository.TenantRepository;
import com.textile.erp.user.repository.UserRepository;
import com.textile.erp.user.service.TenantService;
import com.textile.erp.user.service.UserService;
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
class TenantIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private UUID tenantAId;
    private String tenantASlug;
    private UUID tenantBId;

    private String superAdminToken;
    private String tenantAdminToken;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        long ts = System.currentTimeMillis();

        tenantASlug = "tenant-a-" + ts;
        TenantResponseDto tenantA = tenantService.createTenant(TenantRequestDto.builder()
                .name("Tenant A " + ts)
                .slug(tenantASlug)
                .build());
        tenantAId = tenantA.getId();

        TenantResponseDto tenantB = tenantService.createTenant(TenantRequestDto.builder()
                .name("Tenant B " + ts)
                .slug("tenant-b-" + ts)
                .build());
        tenantBId = tenantB.getId();

        // Super Admin
        User superAdminUser = userRepository.saveAndFlush(User.builder()
                .tenantId(null)
                .email("sa." + ts + "@texforge.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Super")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(superAdminUser.getId(), RoleName.SUPER_ADMIN);

        // Tenant Admin for Tenant A
        User tenantAdminUser = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantAId)
                .email("ta." + ts + "@tenanta.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Tenant")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(tenantAdminUser.getId(), RoleName.TENANT_ADMIN);

        // Employee for Tenant A
        User employeeUser = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantAId)
                .email("emp." + ts + "@tenanta.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Worker")
                .lastName("One")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(employeeUser.getId(), RoleName.EMPLOYEE);

        superAdminToken = jwtService.generateAccessToken(
                superAdminUser.getId(), null, superAdminUser.getEmail(), Set.of(RoleName.SUPER_ADMIN));

        tenantAdminToken = jwtService.generateAccessToken(
                tenantAdminUser.getId(), tenantAId, tenantAdminUser.getEmail(), Set.of(RoleName.TENANT_ADMIN));

        employeeToken = jwtService.generateAccessToken(
                employeeUser.getId(), tenantAId, employeeUser.getEmail(), Set.of(RoleName.EMPLOYEE));
    }

    @Test
    @DisplayName("SUPER_ADMIN can create a new tenant successfully")
    void testSuperAdminCreatesTenant() throws Exception {
        long ts = System.currentTimeMillis();
        TenantRequestDto request = TenantRequestDto.builder()
                .name("Kashmir Loom " + ts)
                .slug("kashmir-loom-" + ts)
                .build();

        mockMvc.perform(post("/api/tenants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Kashmir Loom " + ts))
                .andExpect(jsonPath("$.slug").value("kashmir-loom-" + ts))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("SUPER_ADMIN creates tenant with admin credentials and new admin can log in")
    void testSuperAdminCreatesTenantWithAdminCredentials() throws Exception {
        long ts = System.currentTimeMillis();
        String adminEmail = "owner." + ts + "@surattextiles.com";
        TenantRequestDto request = TenantRequestDto.builder()
                .name("Surat Textiles " + ts)
                .slug("surat-textiles-" + ts)
                .email(adminEmail)
                .password("SuratPass123!")
                .firstName("Suresh")
                .lastName("Patel")
                .build();

        mockMvc.perform(post("/api/tenants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Surat Textiles " + ts))
                .andExpect(jsonPath("$.adminUserId").isNotEmpty())
                .andExpect(jsonPath("$.adminEmail").value(adminEmail));

        // Verify the newly created tenant admin can log in directly at /api/auth/login
        String loginPayload = String.format("{\"email\":\"%s\",\"password\":\"SuratPass123!\"}", adminEmail);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(adminEmail));
    }

    @Test
    @DisplayName("TENANT_ADMIN cannot create a tenant (403 Forbidden)")
    void testTenantAdminCannotCreateTenant() throws Exception {
        TenantRequestDto request = TenantRequestDto.builder()
                .name("Unauthorized Mill")
                .slug("unauthorized-" + System.currentTimeMillis())
                .build();

        mockMvc.perform(post("/api/tenants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request cannot create a tenant (401 Unauthorized)")
    void testUnauthenticatedCannotCreateTenant() throws Exception {
        TenantRequestDto request = TenantRequestDto.builder()
                .name("Anon Mill")
                .slug("anon-mill-" + System.currentTimeMillis())
                .build();

        mockMvc.perform(post("/api/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Creating tenant with duplicate slug fails (400 Bad Request)")
    void testCreateDuplicateSlugFails() throws Exception {
        TenantRequestDto request = TenantRequestDto.builder()
                .name("Duplicate Slug Mill")
                .slug(tenantASlug)
                .build();

        mockMvc.perform(post("/api/tenants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tenant with slug '" + tenantASlug + "' already exists"));
    }

    @Test
    @DisplayName("SUPER_ADMIN can list tenants with search and pagination")
    void testSuperAdminCanListTenants() throws Exception {
        mockMvc.perform(get("/api/tenants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken)
                        .param("search", "Tenant A")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").exists());
    }

    @Test
    @DisplayName("TENANT_ADMIN cannot list all tenants (403 Forbidden)")
    void testTenantAdminCannotListTenants() throws Exception {
        mockMvc.perform(get("/api/tenants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPER_ADMIN can get tenant by ID and Slug")
    void testSuperAdminCanGetTenantByIdAndSlug() throws Exception {
        mockMvc.perform(get("/api/tenants/" + tenantAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenantAId.toString()))
                .andExpect(jsonPath("$.slug").value(tenantASlug));

        mockMvc.perform(get("/api/tenants/slug/" + tenantASlug)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenantAId.toString()));
    }

    @Test
    @DisplayName("TENANT_ADMIN can view own tenant but forbidden for other tenant")
    void testTenantAdminAccessControl() throws Exception {
        // Own tenant -> OK
        mockMvc.perform(get("/api/tenants/" + tenantAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenantAId.toString()));

        // Other tenant -> Forbidden
        mockMvc.perform(get("/api/tenants/" + tenantBId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPER_ADMIN can update tenant profile and status")
    void testSuperAdminUpdateTenantAndStatus() throws Exception {
        UpdateTenantRequest updateRequest = UpdateTenantRequest.builder()
                .name("Tenant A Renamed")
                .build();

        mockMvc.perform(patch("/api/tenants/" + tenantAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tenant A Renamed"));

        UpdateTenantStatusRequest statusRequest = UpdateTenantStatusRequest.builder()
                .status(TenantStatus.SUSPENDED)
                .build();

        mockMvc.perform(patch("/api/tenants/" + tenantAId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("TENANT_ADMIN cannot update tenant status (403 Forbidden)")
    void testTenantAdminCannotUpdateStatus() throws Exception {
        UpdateTenantStatusRequest statusRequest = UpdateTenantStatusRequest.builder()
                .status(TenantStatus.SUSPENDED)
                .build();

        mockMvc.perform(patch("/api/tenants/" + tenantAId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isForbidden());
    }
}
