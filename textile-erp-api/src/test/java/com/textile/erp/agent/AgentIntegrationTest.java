package com.textile.erp.agent;

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
import com.textile.erp.agent.dto.AgentResponse;
import com.textile.erp.agent.dto.CreateAgentRequest;
import com.textile.erp.agent.dto.UpdateAgentRequest;
import com.textile.erp.agent.entity.CommissionType;
import com.textile.erp.auth.service.JwtService;
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
class AgentIntegrationTest {

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
                .name("Agent Tenant A " + ts)
                .slug("agt-tenant-a-" + ts)
                .build());
        tenantAId = tenantA.getId();

        TenantResponseDto tenantB = tenantService.createTenant(TenantRequestDto.builder()
                .name("Agent Tenant B " + ts)
                .slug("agt-tenant-b-" + ts)
                .build());
        tenantBId = tenantB.getId();

        User adminA = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantAId)
                .email("admin." + ts + "@agttenanta.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Agent")
                .lastName("AdminA")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminA.getId(), RoleName.TENANT_ADMIN);

        User adminB = userRepository.saveAndFlush(User.builder()
                .tenantId(tenantBId)
                .email("admin." + ts + "@agttenantb.com")
                .passwordHash(passwordEncoder.encode("Secret123!"))
                .firstName("Agent")
                .lastName("AdminB")
                .status(UserStatus.ACTIVE)
                .build());
        userService.assignRoleToUser(adminB.getId(), RoleName.TENANT_ADMIN);

        tenantAdminAToken = jwtService.generateAccessToken(
                adminA.getId(), tenantAId, adminA.getEmail(), Set.of(RoleName.TENANT_ADMIN));

        tenantAdminBToken = jwtService.generateAccessToken(
                adminB.getId(), tenantBId, adminB.getEmail(), Set.of(RoleName.TENANT_ADMIN));
    }

    private CreateAgentRequest sampleAgent(String name) {
        return CreateAgentRequest.builder()
                .agentName(name)
                .mobile("9825123456")
                .gstin("24AAACA1234A1Z5")
                .pan("AAACA1234A")
                .address("Ring Road Market, Surat")
                .defaultCommissionType(CommissionType.PERCENTAGE)
                .defaultCommissionValue(new BigDecimal("1.50"))
                .active(true)
                .build();
    }

    @Test
    @DisplayName("Should create agent and get by ID")
    void shouldCreateAndGetAgent() throws Exception {
        CreateAgentRequest request = sampleAgent("Vipul Textile Brokerage");

        String res = mockMvc.perform(post("/api/v1/agents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.agentName").value("Vipul Textile Brokerage"))
                .andExpect(jsonPath("$.defaultCommissionType").value("PERCENTAGE"))
                .andExpect(jsonPath("$.defaultCommissionValue").value(1.50))
                .andReturn().getResponse().getContentAsString();

        AgentResponse response = objectMapper.readValue(res, AgentResponse.class);

        mockMvc.perform(get("/api/v1/agents/" + response.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentName").value("Vipul Textile Brokerage"));
    }

    @Test
    @DisplayName("Should list and search agents")
    void shouldListAndSearchAgents() throws Exception {
        mockMvc.perform(post("/api/v1/agents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAgent("Mahesh Agency"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/agents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAgent("Suresh Traders"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/agents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .param("query", "mahesh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].agentName").value("Mahesh Agency"));
    }

    @Test
    @DisplayName("Should update agent and toggle status")
    void shouldUpdateAndToggleAgent() throws Exception {
        String res = mockMvc.perform(post("/api/v1/agents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAgent("Ramesh Broker"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        AgentResponse created = objectMapper.readValue(res, AgentResponse.class);

        UpdateAgentRequest update = UpdateAgentRequest.builder()
                .agentName("Ramesh & Sons Brokers")
                .defaultCommissionType(CommissionType.FIXED)
                .defaultCommissionValue(new BigDecimal("500.00"))
                .active(true)
                .build();

        mockMvc.perform(put("/api/v1/agents/" + created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentName").value("Ramesh & Sons Brokers"))
                .andExpect(jsonPath("$.defaultCommissionType").value("FIXED"))
                .andExpect(jsonPath("$.defaultCommissionValue").value(500.00));

        mockMvc.perform(patch("/api/v1/agents/" + created.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("Should enforce cross-tenant isolation for agents")
    void shouldEnforceCrossTenantIsolation() throws Exception {
        String res = mockMvc.perform(post("/api/v1/agents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleAgent("Tenant A Agent"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        AgentResponse created = objectMapper.readValue(res, AgentResponse.class);

        // Tenant B cannot get
        mockMvc.perform(get("/api/v1/agents/" + created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminBToken))
                .andExpect(status().isNotFound());

        // Tenant B cannot delete
        mockMvc.perform(delete("/api/v1/agents/" + created.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should reject invalid GSTIN and PAN format with 400 Bad Request")
    void shouldRejectInvalidFormats() throws Exception {
        CreateAgentRequest invalid = sampleAgent("Bad Format Agent");
        invalid.setGstin("INVALID_GST");

        mockMvc.perform(post("/api/v1/agents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAdminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}
