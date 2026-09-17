package com.textile.erp.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "System Health", description = "Service health and liveness monitoring endpoints")
public class HealthController {

    @GetMapping({"/api/v1/health", "/api/health", "/health"})
    @Operation(
            summary = "Health Check",
            description = "Returns current operational status, service name, and timestamp. Unauthenticated public endpoint used for container orchestration and uptime monitoring."
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy and operational")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "textile-erp-api");
        response.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(response);
    }
}
