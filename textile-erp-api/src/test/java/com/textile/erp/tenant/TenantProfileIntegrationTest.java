package com.textile.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.textile.erp.auth.service.JwtService;
import com.textile.erp.tenant.dto.UpdateTenantProfileRequest;
import com.textile.erp.tenant.entity.Address;
import com.textile.erp.tenant.entity.BankAccountType;
import com.textile.erp.tenant.entity.BankInfo;
import com.textile.erp.tenant.entity.CompanyIdentity;
import com.textile.erp.tenant.entity.CompanyType;
import com.textile.erp.tenant.entity.ContactInfo;
import com.textile.erp.tenant.entity.ErpSettings;
import com.textile.erp.tenant.entity.InvoiceConfig;
import com.textile.erp.tenant.entity.MeasurementUnit;
import com.textile.erp.tenant.entity.TaxInfo;
import com.textile.erp.tenant.entity.TextileBusinessInfo;
import com.textile.erp.user.dto.TenantRequestDto;
import com.textile.erp.user.dto.TenantResponseDto;
import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.User;
import com.textile.erp.user.entity.UserStatus;
import com.textile.erp.user.repository.UserRepository;
import com.textile.erp.user.service.TenantService;
import com.textile.erp.user.service.UserService;
import java.time.LocalDate;
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
class TenantProfileIntegrationTest {

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

    private String superAdminToken;
    private String tenantAdminAToken;
    private String employeeAToken;
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
                .name("Silk Mills A " + ts)
                .slug("silk-mills-a-" + ts)
                .build());
        tenantAId = tenantA.getId();

        TenantResponseDto tenantB = tenantService.createTenant(TenantRequestDto.builder()
                .name("Cotton Mills B " + ts)
                .slug("cotton-mills-b-" + ts)
                .build());
        tenantBId = tenantB.getId();

        // Platform Super Admin
        User superAdmin = userRepository.saveAndFlush(User.builder()
                .tenantId(null)
                .email("sa." + ts + "@texforge.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Super")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(superAdmin.getId(), RoleName.SUPER_ADMIN);

        // Tenant Admin for Tenant A
        User tenantAdminA = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantAId)
                .email("admin." + ts + "@silkmills.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Raj")
                .lastName("Shah")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(tenantAdminA.getId(), RoleName.TENANT_ADMIN);

        // Employee for Tenant A
        User employeeA = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantAId)
                .email("emp." + ts + "@silkmills.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Karan")
                .lastName("Patel")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(employeeA.getId(), RoleName.EMPLOYEE);

        // Tenant Admin for Tenant B
        User tenantAdminB = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantBId)
                .email("admin." + ts + "@cottonmills.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Vikas")
                .lastName("Sharma")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(tenantAdminB.getId(), RoleName.TENANT_ADMIN);

        superAdminToken = jwtService.generateAccessToken(
                superAdmin.getId(), null, superAdmin.getEmail(), Set.of(RoleName.SUPER_ADMIN));

        tenantAdminAToken = jwtService.generateAccessToken(
                tenantAdminA.getId(), tenantAId, tenantAdminA.getEmail(), Set.of(RoleName.TENANT_ADMIN));

        employeeAToken = jwtService.generateAccessToken(
                employeeA.getId(), tenantAId, employeeA.getEmail(), Set.of(RoleName.EMPLOYEE));

        tenantAdminBToken = jwtService.generateAccessToken(
                tenantAdminB.getId(), tenantBId, tenantAdminB.getEmail(), Set.of(RoleName.TENANT_ADMIN));
    }

    @Test
    @DisplayName("GET /api/tenant/profile initializes default profile and returns 200 OK")
    void testGetDefaultTenantProfile() throws Exception {
        mockMvc.perform(get("/api/tenant/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantAId.toString()))
                .andExpect(jsonPath("$.companyIdentity.legalName").isNotEmpty())
                .andExpect(jsonPath("$.erpSettings.defaultCurrency").value("INR"));
    }

    @Test
    @DisplayName("PUT /api/tenant/profile updates all 8 business configuration areas")
    void testUpdateAllConfigurationSections() throws Exception {
        UpdateTenantProfileRequest request = UpdateTenantProfileRequest.builder()
                .companyIdentity(CompanyIdentity.builder()
                        .legalName("Silk Mills Private Limited")
                        .tradeName("SilkMills India")
                        .logoUrl("https://assets.silkmills.com/logo.png")
                        .website("https://silkmills.com")
                        .incorporationDate(LocalDate.of(2015, 6, 15))
                        .companyType(CompanyType.PRIVATE_LIMITED)
                        .build())
                .registeredAddress(Address.builder()
                        .addressLine1("Plot 42, GIDC Industrial Estate")
                        .addressLine2("Near Ring Road")
                        .city("Surat")
                        .state("Gujarat")
                        .stateCode("24")
                        .postalCode("395002")
                        .country("India")
                        .build())
                .contactInfo(ContactInfo.builder()
                        .primaryEmail("contact@silkmills.com")
                        .primaryPhone("+91-9876543210")
                        .secondaryPhone("+91-261-2345678")
                        .supportEmail("support@silkmills.com")
                        .build())
                .taxInfo(TaxInfo.builder()
                        .gstin("24AAACS1234F1Z5")
                        .pan("AAACS1234F")
                        .tan("SRTS12345F")
                        .reverseChargeApplicable(false)
                        .compositionScheme(false)
                        .lutNumber("AD240324001234X")
                        .build())
                .bankInfo(BankInfo.builder()
                        .bankName("State Bank of India")
                        .accountNumber("123456789012")
                        .ifscCode("SBIN0001234")
                        .branchName("Ring Road, Surat")
                        .accountType(BankAccountType.CURRENT)
                        .upiId("silkmills@sbi")
                        .build())
                .textileBusinessInfo(TextileBusinessInfo.builder()
                        .businessTypes("WEAVING, DYEING")
                        .millCapacityDetails("150 Airjet Looms, 20000 Meters/Day")
                        .standardMeasurementUnit(MeasurementUnit.METERS)
                        .loomTypes("Tsudakoma ZAX9100")
                        .build())
                .invoiceConfig(InvoiceConfig.builder()
                        .invoicePrefix("SM-24-")
                        .invoiceStartingSequence(1001L)
                        .defaultPaymentTermsDays(45)
                        .termsAndConditions("1. Goods once sold will not be taken back. 2. Subject to Surat jurisdiction.")
                        .declarationText("We declare that this invoice shows the actual price of the goods described.")
                        .signatureImageUrl("https://assets.silkmills.com/sign.png")
                        .build())
                .erpSettings(ErpSettings.builder()
                        .fiscalYearStartMonth(4)
                        .defaultCurrency("INR")
                        .timeZone("Asia/Kolkata")
                        .dateFormat("DD/MM/YYYY")
                        .build())
                .build();

        mockMvc.perform(put("/api/tenant/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyIdentity.legalName").value("Silk Mills Private Limited"))
                .andExpect(jsonPath("$.companyIdentity.companyType").value("PRIVATE_LIMITED"))
                .andExpect(jsonPath("$.registeredAddress.city").value("Surat"))
                .andExpect(jsonPath("$.taxInfo.gstin").value("24AAACS1234F1Z5"))
                .andExpect(jsonPath("$.bankInfo.accountType").value("CURRENT"))
                .andExpect(jsonPath("$.textileBusinessInfo.standardMeasurementUnit").value("METERS"))
                .andExpect(jsonPath("$.invoiceConfig.invoicePrefix").value("SM-24-"))
                .andExpect(jsonPath("$.invoiceConfig.invoiceStartingSequence").value(1001))
                .andExpect(jsonPath("$.erpSettings.defaultCurrency").value("INR"));

        // Verify changes persist
        mockMvc.perform(get("/api/tenant/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyIdentity.legalName").value("Silk Mills Private Limited"))
                .andExpect(jsonPath("$.taxInfo.gstin").value("24AAACS1234F1Z5"));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Tenant B cannot access or update Tenant A profile (403 Forbidden)")
    void testCrossTenantAccessDenied() throws Exception {
        // Read attempt across tenants
        mockMvc.perform(get("/api/tenant/profile/" + tenantAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminBToken))
                .andExpect(status().isForbidden());

        // Update attempt across tenants
        UpdateTenantProfileRequest request = UpdateTenantProfileRequest.builder()
                .companyIdentity(CompanyIdentity.builder().tradeName("Hacked Name").build())
                .build();

        mockMvc.perform(put("/api/tenant/profile/" + tenantAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EMPLOYEE can read own profile but cannot update it (403 Forbidden)")
    void testEmployeePermissions() throws Exception {
        // Employee can read
        mockMvc.perform(get("/api/tenant/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeAToken))
                .andExpect(status().isOk());

        // Employee cannot update
        UpdateTenantProfileRequest request = UpdateTenantProfileRequest.builder()
                .companyIdentity(CompanyIdentity.builder().tradeName("Unauthorized Change").build())
                .build();

        mockMvc.perform(put("/api/tenant/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPER_ADMIN can view and update any tenant profile via tenantId")
    void testSuperAdminAccess() throws Exception {
        // Super Admin views Tenant A
        mockMvc.perform(get("/api/tenant/profile/" + tenantAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantAId.toString()));

        // Super Admin updates Tenant A
        UpdateTenantProfileRequest request = UpdateTenantProfileRequest.builder()
                .companyIdentity(CompanyIdentity.builder().tradeName("Super Admin Updated").build())
                .build();

        mockMvc.perform(put("/api/tenant/profile/" + tenantAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyIdentity.tradeName").value("Super Admin Updated"));
    }

    @Test
    @DisplayName("Unauthenticated request receives 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/tenant/profile"))
                .andExpect(status().isUnauthorized());
    }
}
