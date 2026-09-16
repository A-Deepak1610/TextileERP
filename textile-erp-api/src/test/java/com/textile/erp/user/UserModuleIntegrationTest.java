package com.textile.erp.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.textile.erp.auth.security.CurrentUser;
import com.textile.erp.auth.service.JwtService;
import com.textile.erp.user.dto.AssignRoleRequest;
import com.textile.erp.user.dto.CreateUserRequest;
import com.textile.erp.user.dto.TenantRequestDto;
import com.textile.erp.user.dto.TenantResponseDto;
import com.textile.erp.user.dto.UpdateUserRequest;
import com.textile.erp.user.dto.UpdateUserStatusRequest;
import com.textile.erp.user.dto.UserResponse;
import com.textile.erp.user.entity.Role;
import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.User;
import com.textile.erp.user.entity.UserStatus;
import com.textile.erp.user.repository.RoleRepository;
import com.textile.erp.user.repository.UserRepository;
import com.textile.erp.user.repository.UserRoleRepository;
import com.textile.erp.user.service.TenantService;
import com.textile.erp.user.service.UserService;
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
class UserModuleIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserService userService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private UUID tenantAId;
    private UUID tenantBId;
    private User superAdminUser;
    private User tenantAdminUser;
    private User employeeUser;
    private User tenantBUser;

    private String superAdminToken;
    private String tenantAdminToken;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        long ts = System.currentTimeMillis();

        TenantResponseDto tenantA = tenantService.createTenant(TenantRequestDto.builder()
                .name("Alpha Mills " + ts)
                .slug("alpha-mills-" + ts)
                .build());
        tenantAId = tenantA.getId();

        TenantResponseDto tenantB = tenantService.createTenant(TenantRequestDto.builder()
                .name("Beta Silks " + ts)
                .slug("beta-silks-" + ts)
                .build());
        tenantBId = tenantB.getId();

        // Platform Super Admin
        superAdminUser = userRepository.saveAndFlush(User.builder()
                .tenantId(null)
                .email("superadmin." + ts + "@texforge.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Global")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(superAdminUser.getId(), RoleName.SUPER_ADMIN);

        // Tenant Admin for Tenant A
        tenantAdminUser = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantAId)
                .email("admin." + ts + "@alpha.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Alpha")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(tenantAdminUser.getId(), RoleName.TENANT_ADMIN);

        // Employee for Tenant A
        employeeUser = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantAId)
                .email("emp." + ts + "@alpha.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Alpha")
                .lastName("Worker")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(employeeUser.getId(), RoleName.EMPLOYEE);

        // Employee for Tenant B
        tenantBUser = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantBId)
                .email("emp." + ts + "@beta.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Beta")
                .lastName("Worker")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(tenantBUser.getId(), RoleName.EMPLOYEE);

        // JWT tokens
        superAdminToken = jwtService.generateAccessToken(
                superAdminUser.getId(), null, superAdminUser.getEmail(), Set.of(RoleName.SUPER_ADMIN));

        tenantAdminToken = jwtService.generateAccessToken(
                tenantAdminUser.getId(), tenantAId, tenantAdminUser.getEmail(), Set.of(RoleName.TENANT_ADMIN));

        employeeToken = jwtService.generateAccessToken(
                employeeUser.getId(), tenantAId, employeeUser.getEmail(), Set.of(RoleName.EMPLOYEE));
    }

    @Test
    @DisplayName("GET /api/users/me returns authenticated user profile")
    void testGetCurrentUserProfile() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenantAdminUser.getId().toString()))
                .andExpect(jsonPath("$.email").value(tenantAdminUser.getEmail()))
                .andExpect(jsonPath("$.roles[0]").value("TENANT_ADMIN"))
                .andExpect(jsonPath("$.tenantId").value(tenantAId.toString()));
    }

    @Test
    @DisplayName("POST /api/users - Super Admin can provision tenant admin for a tenant")
    void testSuperAdminProvisionTenantUser() throws Exception {
        String email = "manager." + System.currentTimeMillis() + "@alpha.com";
        CreateUserRequest request = CreateUserRequest.builder()
                .tenantId(tenantAId)
                .email(email)
                .password("StrongPass123!")
                .firstName("Store")
                .lastName("Manager")
                .role(RoleName.TENANT_ADMIN)
                .build();

        MvcResult result = mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.tenantId").value(tenantAId.toString()))
                .andExpect(jsonPath("$.roles[0]").value("TENANT_ADMIN"))
                .andReturn();

        User createdUser = userRepository.findByTenantIdAndEmail(tenantAId, email).orElseThrow();
        // Assert password was hashed with BCrypt and not stored plaintext
        assertThat(createdUser.getPasswordHash()).isNotEqualTo("StrongPass123!");
        assertThat(passwordEncoder.matches("StrongPass123!", createdUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("POST /api/users - Tenant Admin can provision employee within own tenant")
    void testTenantAdminProvisionEmployee() throws Exception {
        String email = "operator." + System.currentTimeMillis() + "@alpha.com";
        // Even if client maliciously sends another tenantId, service forces caller's tenantId
        CreateUserRequest request = CreateUserRequest.builder()
                .tenantId(tenantBId)
                .email(email)
                .password("StrongPass123!")
                .firstName("Machine")
                .lastName("Operator")
                .role(RoleName.EMPLOYEE)
                .build();

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.tenantId").value(tenantAId.toString()))
                .andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"));
    }

    @Test
    @DisplayName("POST /api/users - Tenant Admin cannot create SUPER_ADMIN")
    void testTenantAdminCannotCreateSuperAdmin() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("illegal.super." + System.currentTimeMillis() + "@texforge.com")
                .password("StrongPass123!")
                .firstName("Fake")
                .lastName("SuperAdmin")
                .role(RoleName.SUPER_ADMIN)
                .build();

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("POST /api/users - Employee cannot provision users")
    void testEmployeeCannotProvisionUsers() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("unauthorized." + System.currentTimeMillis() + "@alpha.com")
                .password("StrongPass123!")
                .firstName("Unauthorized")
                .lastName("User")
                .role(RoleName.EMPLOYEE)
                .build();

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/users/{id} - Cross-tenant isolation blocks Tenant Admin from other tenant users")
    void testTenantAdminCrossTenantAccessForbidden() throws Exception {
        mockMvc.perform(get("/api/users/" + tenantBUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cross-tenant access forbidden")));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Tenant Admin cannot view Platform Super Admin")
    void testTenantAdminCannotViewSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/users/" + superAdminUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Tenant administrators cannot view or modify platform-level users")));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Employee cannot view other user records")
    void testEmployeeCannotViewOtherUsers() throws Exception {
        mockMvc.perform(get("/api/users/" + tenantAdminUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/users - Tenant Admin lists users strictly for their own tenant")
    void testTenantAdminListUsersScoped() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminToken)
                        .param("search", "Alpha")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.email == '" + tenantBUser.getEmail() + "')]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.email == '" + superAdminUser.getEmail() + "')]").doesNotExist());
    }

    @Test
    @DisplayName("PATCH /api/users/{id} - Updates profile first and last name")
    void testUpdateUserProfile() throws Exception {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("UpdatedAlpha")
                .lastName("UpdatedWorker")
                .build();

        mockMvc.perform(patch("/api/users/" + employeeUser.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("UpdatedAlpha"))
                .andExpect(jsonPath("$.lastName").value("UpdatedWorker"));
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/status - Updates user status to INACTIVE")
    void testUpdateUserStatus() throws Exception {
        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .status(UserStatus.INACTIVE)
                .build();

        mockMvc.perform(patch("/api/users/" + employeeUser.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("POST /api/users/{id}/roles and DELETE /api/users/{id}/roles/{roleId} - Assign and remove role")
    void testAssignAndRemoveRole() throws Exception {
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE).orElseThrow();

        // Assign EMPLOYEE role to tenantAdminUser
        AssignRoleRequest assignReq = AssignRoleRequest.builder()
                .roleName(RoleName.EMPLOYEE)
                .build();

        mockMvc.perform(post("/api/users/" + tenantAdminUser.getId() + "/roles")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.hasItem("EMPLOYEE")));

        // Now remove EMPLOYEE role
        mockMvc.perform(delete("/api/users/" + tenantAdminUser.getId() + "/roles/" + employeeRole.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("EMPLOYEE"))));
    }

    @Test
    @DisplayName("DELETE /api/users/{id}/roles/{roleId} - Reject removing last remaining role")
    void testCannotRemoveLastRemainingRole() {
        Role employeeRole = roleRepository.findByName(RoleName.EMPLOYEE).orElseThrow();

        // Set security context as super admin
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new CurrentUser(superAdminUser.getId(), null, superAdminUser.getEmail(), Set.of(RoleName.SUPER_ADMIN)),
                        null,
                        List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                )
        );

        assertThatThrownBy(() -> userService.removeRole(employeeUser.getId(), employeeRole.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot remove the last remaining role");

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
