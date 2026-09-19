package com.tamvagbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/v1/health")
@Tag(name = "Platform Operations", description = "Render deployment health checks and uptime monitoring")
public class HealthController {

    @GetMapping
    @Operation(
        summary = "Health check",
        description = "Returns the current API health status",
        security = {}
    )
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "tamva-backend",
                "version", "1.0.0",
                "region", "Ghana / Africa-Ready",
                "timestamp", Instant.now().toString()
        ));
    }
}
