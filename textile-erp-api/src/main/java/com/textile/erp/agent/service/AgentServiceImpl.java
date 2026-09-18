package com.textile.erp.agent.service;

import com.textile.erp.agent.dto.CreateAgentRequest;
import com.textile.erp.agent.dto.AgentResponse;
import com.textile.erp.agent.dto.UpdateAgentRequest;
import com.textile.erp.agent.entity.Agent;
import com.textile.erp.agent.entity.CommissionType;
import com.textile.erp.agent.repository.AgentRepository;
import com.textile.erp.auth.model.CurrentUser;
import com.textile.erp.auth.security.SecurityUtils;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");

    private final AgentRepository agentRepository;

    @Override
    @Transactional
    public AgentResponse createAgent(CreateAgentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateAgentRequest cannot be null");
        }

        UUID tenantId = resolveCurrentTenantId();
        validateFormats(request.getGstin(), request.getPan());

        Agent agent = Agent.builder()
                .tenantId(tenantId)
                .agentName(request.getAgentName().trim())
                .mobile(trimOrNull(request.getMobile()))
                .gstin(normalizeUpper(request.getGstin()))
                .pan(normalizeUpper(request.getPan()))
                .address(trimOrNull(request.getAddress()))
                .defaultCommissionType(request.getDefaultCommissionType() != null ? request.getDefaultCommissionType() : CommissionType.PERCENTAGE)
                .defaultCommissionValue(request.getDefaultCommissionValue() != null ? request.getDefaultCommissionValue() : BigDecimal.ZERO)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Agent saved = agentRepository.saveAndFlush(agent);
        log.info("Created agent {} (ID: {}) for tenant {}", saved.getAgentName(), saved.getId(), tenantId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentResponse getAgentById(UUID id) {
        UUID tenantId = resolveCurrentTenantId();
        Agent agent = findSecured(tenantId, id);
        return mapToResponse(agent);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgentResponse> listAgents(String query, Boolean active, Pageable pageable) {
        UUID tenantId = resolveCurrentTenantId();
        String term = (query != null && !query.isBlank()) ? query.trim() : null;
        return agentRepository.searchAgents(tenantId, term, active, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public AgentResponse updateAgent(UUID id, UpdateAgentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("UpdateAgentRequest cannot be null");
        }

        UUID tenantId = resolveCurrentTenantId();
        Agent agent = findSecured(tenantId, id);
        validateFormats(request.getGstin(), request.getPan());

        if (request.getAgentName() != null && !request.getAgentName().isBlank()) {
            agent.setAgentName(request.getAgentName().trim());
        }
        if (request.getMobile() != null) {
            agent.setMobile(trimOrNull(request.getMobile()));
        }
        if (request.getGstin() != null) {
            agent.setGstin(normalizeUpper(request.getGstin()));
        }
        if (request.getPan() != null) {
            agent.setPan(normalizeUpper(request.getPan()));
        }
        if (request.getAddress() != null) {
            agent.setAddress(trimOrNull(request.getAddress()));
        }
        if (request.getDefaultCommissionType() != null) {
            agent.setDefaultCommissionType(request.getDefaultCommissionType());
        }
        if (request.getDefaultCommissionValue() != null) {
            agent.setDefaultCommissionValue(request.getDefaultCommissionValue());
        }
        if (request.getActive() != null) {
            agent.setActive(request.getActive());
        }

        Agent updated = agentRepository.saveAndFlush(agent);
        log.info("Updated agent {} (ID: {}) for tenant {}", updated.getAgentName(), updated.getId(), tenantId);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public AgentResponse toggleAgentStatus(UUID id, Boolean active) {
        UUID tenantId = resolveCurrentTenantId();
        Agent agent = findSecured(tenantId, id);
        agent.setActive(Boolean.TRUE.equals(active));
        Agent updated = agentRepository.saveAndFlush(agent);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteAgent(UUID id) {
        UUID tenantId = resolveCurrentTenantId();
        Agent agent = findSecured(tenantId, id);
        agentRepository.delete(agent);
        log.info("Deleted agent {} (ID: {}) for tenant {}", agent.getAgentName(), id, tenantId);
    }

    private Agent findSecured(UUID tenantId, UUID id) {
        return agentRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new NoSuchElementException("Agent not found with ID: " + id));
    }

    private UUID resolveCurrentTenantId() {
        CurrentUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));
        if (currentUser.getTenantId() != null) {
            return currentUser.getTenantId();
        }
        throw new IllegalArgumentException("Action requires an active tenant context");
    }

    private void validateFormats(String gstin, String pan) {
        if (gstin != null && !gstin.trim().isEmpty() && !GSTIN_PATTERN.matcher(gstin.trim().toUpperCase()).matches()) {
            throw new IllegalArgumentException("Invalid GSTIN format: '" + gstin + "'");
        }
        if (pan != null && !pan.trim().isEmpty() && !PAN_PATTERN.matcher(pan.trim().toUpperCase()).matches()) {
            throw new IllegalArgumentException("Invalid PAN format: '" + pan + "'");
        }
    }

    private String trimOrNull(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        return str.trim();
    }

    private String normalizeUpper(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        return str.trim().toUpperCase();
    }

    private AgentResponse mapToResponse(Agent agent) {
        return AgentResponse.builder()
                .id(agent.getId())
                .tenantId(agent.getTenantId())
                .agentName(agent.getAgentName())
                .mobile(agent.getMobile())
                .gstin(agent.getGstin())
                .pan(agent.getPan())
                .address(agent.getAddress())
                .defaultCommissionType(agent.getDefaultCommissionType())
                .defaultCommissionValue(agent.getDefaultCommissionValue())
                .active(agent.getActive())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }
}
