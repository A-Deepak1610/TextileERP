package com.textile.erp.agent.controller;

import com.textile.erp.agent.dto.CreateAgentRequest;
import com.textile.erp.agent.dto.AgentResponse;
import com.textile.erp.agent.dto.UpdateAgentRequest;
import com.textile.erp.agent.service.AgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agent Management", description = "Agent / Broker master and commission profile management")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final AgentService agentService;

    @PostMapping
    @Operation(summary = "Create agent", description = "Add a new sales agent or textile broker to the tenant master.")
    @ApiResponse(responseCode = "201", description = "Agent created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or validation failed")
    public ResponseEntity<AgentResponse> createAgent(@Valid @RequestBody CreateAgentRequest request) {
        AgentResponse response = agentService.createAgent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List agents", description = "Fetch paginated sales agents with optional query search and active status filter.")
    @ApiResponse(responseCode = "200", description = "Agents page returned")
    public ResponseEntity<Page<AgentResponse>> listAgents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<AgentResponse> result = agentService.listAgents(query, active, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get agent by ID", description = "Fetch agent details by ID for current tenant.")
    @ApiResponse(responseCode = "200", description = "Agent returned")
    @ApiResponse(responseCode = "404", description = "Agent not found")
    public ResponseEntity<AgentResponse> getAgentById(@PathVariable UUID id) {
        AgentResponse response = agentService.getAgentById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update agent", description = "Update agent details and commission parameters.")
    @ApiResponse(responseCode = "200", description = "Agent updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or validation failed")
    @ApiResponse(responseCode = "404", description = "Agent not found")
    public ResponseEntity<AgentResponse> updateAgent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAgentRequest request) {
        AgentResponse response = agentService.updateAgent(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle agent active status", description = "Activate or deactivate a sales agent.")
    @ApiResponse(responseCode = "200", description = "Agent status updated")
    @ApiResponse(responseCode = "404", description = "Agent not found")
    public ResponseEntity<AgentResponse> toggleAgentStatus(
            @PathVariable UUID id,
            @RequestParam Boolean active) {
        AgentResponse response = agentService.toggleAgentStatus(id, active);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete agent", description = "Delete an agent from the tenant directory.")
    @ApiResponse(responseCode = "204", description = "Agent deleted")
    @ApiResponse(responseCode = "404", description = "Agent not found")
    public ResponseEntity<Void> deleteAgent(@PathVariable UUID id) {
        agentService.deleteAgent(id);
        return ResponseEntity.noContent().build();
    }
}
