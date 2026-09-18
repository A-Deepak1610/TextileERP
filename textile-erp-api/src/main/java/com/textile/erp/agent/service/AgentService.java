package com.textile.erp.agent.service;

import com.textile.erp.agent.dto.CreateAgentRequest;
import com.textile.erp.agent.dto.AgentResponse;
import com.textile.erp.agent.dto.UpdateAgentRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AgentService {

    AgentResponse createAgent(CreateAgentRequest request);

    AgentResponse getAgentById(UUID id);

    Page<AgentResponse> listAgents(String query, Boolean active, Pageable pageable);

    AgentResponse updateAgent(UUID id, UpdateAgentRequest request);

    AgentResponse toggleAgentStatus(UUID id, Boolean active);

    void deleteAgent(UUID id);
}
