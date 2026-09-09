package com.textile.erp.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.textile.erp.auth.dto.LoginRequestDto;
import com.textile.erp.auth.dto.LogoutRequestDto;
import com.textile.erp.auth.dto.RefreshTokenRequestDto;
import com.textile.erp.auth.security.CurrentUser;
import com.textile.erp.auth.service.JwtService;
import com.textile.erp.user.dto.TenantRequestDto;
import com.textile.erp.user.dto.TenantResponseDto;
import com.textile.erp.user.dto.UserCreateRequestDto;
import com.textile.erp.user.dto.UserResponseDto;
import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.User;
import com.textile.erp.user.entity.UserStatus;
import com.textile.erp.user.repository.UserRepository;
import com.textile.erp.user.service.TenantService;
import com.textile.erp.user.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class AuthIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserService userService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Verify seeded superadmin can log in using email admin@gmail.com and password admin@123")
    void testSeededSuperAdminLoginWithEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@gmail.com\",\"password\":\"admin@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("admin@gmail.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("SUPER_ADMIN"));
    }

    @Test
    @DisplayName("Successful login for SUPER_ADMIN produces valid tokens with tenant_id = null")
    void testSuperAdminLoginAndJwtClaims() throws Exception {
        String email = "superadmin." + System.currentTimeMillis() + "@texforge.com";
        String password = "SecretSuperPassword123!";

        userService.createUser(UserCreateRequestDto.builder()
                .email(email)
                .password(password)
                .firstName("Super")
                .lastName("Admin")
                .role(RoleName.SUPER_ADMIN)
                .tenantId(null)
                .build());

        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.tenantId").isEmpty())
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = responseJson.get("accessToken").asText();

        // Validate JWT structure and claims
        assertThat(jwtService.validateToken(accessToken)).isTrue();
        CurrentUser currentUser = jwtService.extractCurrentUser(accessToken);
        assertThat(currentUser.getEmail()).isEqualTo(email);
        assertThat(currentUser.getTenantId()).isNull();
        assertThat(currentUser.isPlatformUser()).isTrue();
        assertThat(currentUser.isSuperAdmin()).isTrue();
        assertThat(currentUser.hasRole(RoleName.SUPER_ADMIN)).isTrue();
    }

    @Test
    @DisplayName("Successful login for TENANT_ADMIN produces JWT containing correct tenant_id and roles")
    void testTenantAdminLoginAndJwtClaims() throws Exception {
        long ts = System.currentTimeMillis();
        TenantResponseDto tenant = tenantService.createTenant(TenantRequestDto.builder()
                .name("Alpha Mills " + ts)
                .slug("alpha-mills-" + ts)
                .build());

        String email = "tenantadmin." + ts + "@alphamills.com";
        String password = "TenantAdminPass456!";

        userService.createUser(UserCreateRequestDto.builder()
                .email(email)
                .password(password)
                .firstName("Tenant")
                .lastName("Admin")
                .role(RoleName.TENANT_ADMIN)
                .tenantId(tenant.getId())
                .build());

        LoginRequestDto loginRequest = LoginRequestDto.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.tenantId").value(tenant.getId().toString()))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = responseJson.get("accessToken").asText();

        CurrentUser currentUser = jwtService.extractCurrentUser(accessToken);
        assertThat(currentUser.getTenantId()).isEqualTo(tenant.getId());
        assertThat(currentUser.isPlatformUser()).isFalse();
        assertThat(currentUser.hasRole(RoleName.TENANT_ADMIN)).isTrue();
    }

    @Test
    @DisplayName("Login with invalid password or unknown email returns 401 Unauthorized")
    void testInvalidCredentialsLogin() throws Exception {
        String email = "validuser." + System.currentTimeMillis() + "@texforge.com";
        userService.createUser(UserCreateRequestDto.builder()
                .email(email)
                .password("CorrectPassword1!")
                .firstName("Valid")
                .lastName("User")
                .role(RoleName.SUPER_ADMIN)
                .tenantId(null)
                .build());

        // Wrong password
        LoginRequestDto wrongPass = LoginRequestDto.builder()
                .email(email)
                .password("WrongPassword999!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPass)))
                .andExpect(status().isUnauthorized());

        // Unknown email
        LoginRequestDto unknownEmail = LoginRequestDto.builder()
                .email("nonexistent.user@texforge.com")
                .password("AnyPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unknownEmail)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Inactive user login is rejected with 403 Forbidden")
    void testInactiveUserLoginRejected() throws Exception {
        String email = "inactive." + System.currentTimeMillis() + "@texforge.com";
        UserResponseDto created = userService.createUser(UserCreateRequestDto.builder()
                .email(email)
                .password("Password123!")
                .firstName("Inactive")
                .lastName("User")
                .role(RoleName.SUPER_ADMIN)
                .tenantId(null)
                .build());

        // Set user status to INACTIVE
        User user = userRepository.findById(created.getId()).orElseThrow();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.saveAndFlush(user);

        LoginRequestDto request = LoginRequestDto.builder()
                .email(email)
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User account is INACTIVE"));
    }

    @Test
    @DisplayName("Refresh token rotation succeeds and invalidates the previous token")
    void testRefreshTokenRotationAndRevocation() throws Exception {
        String email = "rotatetest." + System.currentTimeMillis() + "@texforge.com";
        String password = "RotationPass123!";

        userService.createUser(UserCreateRequestDto.builder()
                .email(email)
                .password(password)
                .firstName("Rotate")
                .lastName("Tester")
                .role(RoleName.SUPER_ADMIN)
                .tenantId(null)
                .build());

        // 1. Initial login
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequestDto.builder()
                        .email(email)
                        .password(password)
                        .build())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String originalRefreshToken = loginJson.get("refreshToken").asText();

        // 2. Refresh tokens
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RefreshTokenRequestDto.builder()
                        .refreshToken(originalRefreshToken)
                        .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshJson = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshJson.get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(originalRefreshToken);

        // 3. Old refresh token should now be revoked
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RefreshTokenRequestDto.builder()
                        .refreshToken(originalRefreshToken)
                        .build())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
    }

    @Test
    @DisplayName("Logout revokes the refresh token and prevents future refresh")
    void testLogoutRevokesRefreshToken() throws Exception {
        String email = "logouttest." + System.currentTimeMillis() + "@texforge.com";
        String password = "LogoutPass123!";

        userService.createUser(UserCreateRequestDto.builder()
                .email(email)
                .password(password)
                .firstName("Logout")
                .lastName("User")
                .role(RoleName.SUPER_ADMIN)
                .tenantId(null)
                .build());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequestDto.builder()
                        .email(email)
                        .password(password)
                        .build())))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = loginJson.get("refreshToken").asText();

        // Logout
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LogoutRequestDto.builder()
                        .refreshToken(refreshToken)
                        .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        // Attempt refresh after logout
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RefreshTokenRequestDto.builder()
                        .refreshToken(refreshToken)
                        .build())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
    }

    @Test
    @DisplayName("Protected endpoint access with JWT vs unauthorized access")
    void testProtectedEndpointAccessWithJwt() throws Exception {
        String email = "jwtprotect." + System.currentTimeMillis() + "@texforge.com";
        String password = "ProtectPass123!";

        userService.createUser(UserCreateRequestDto.builder()
                .email(email)
                .password(password)
                .firstName("Protected")
                .lastName("Endpoint")
                .role(RoleName.SUPER_ADMIN)
                .tenantId(null)
                .build());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(LoginRequestDto.builder()
                        .email(email)
                        .password(password)
                        .build())))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // 1. Without token to protected health endpoint? No, health is public (200).
        // To a non-public URL (e.g. /api/v1/users):
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());

        // 2. With valid JWT Bearer header:
        // /api/v1/users does not have a registered controller endpoint yet (so it yields 404 or passes security to DispatcherServlet),
        // but crucially, it is NOT 401 Unauthorized because authentication succeeded!
        MvcResult authedResult = mockMvc.perform(get("/api/v1/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andReturn();

        assertThat(authedResult.getResponse().getStatus()).isNotEqualTo(401);
    }
}
