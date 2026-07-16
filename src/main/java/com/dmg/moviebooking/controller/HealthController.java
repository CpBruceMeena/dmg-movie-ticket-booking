package com.dmg.moviebooking.controller;

import com.dmg.moviebooking.dto.response.HealthResponse;
import com.dmg.moviebooking.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "Service health check endpoint")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/api/health")
    @Operation(summary = "Check the health status of all backend services")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = healthService.checkHealth();
        return ResponseEntity.ok(response);
    }
}
