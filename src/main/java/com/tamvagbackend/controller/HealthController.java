package com.tamvagbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/health")
@Tag(name = "Platform Operations", description = "Readiness, liveness, and infrastructure health monitoring")
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;

    public HealthController(DataSource dataSource, StringRedisTemplate redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping
    @Operation(
            summary = "Readiness health check endpoint",
            description = "Validates that PostgreSQL database and Redis dependencies are ready to serve traffic",
            security = {}
    )
    public ResponseEntity<Map<String, Object>> checkHealth() {
        boolean dbHealthy = isDatabaseHealthy();
        boolean redisHealthy = isRedisHealthy();

        boolean isUp = dbHealthy && redisHealthy;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", isUp ? "UP" : "DOWN");
        response.put("service", "tamva-backend");
        response.put("version", "1.0.0");
        response.put("timestamp", Instant.now().toString());

        Map<String, Object> components = new LinkedHashMap<>();
        components.put("database", dbHealthy ? Map.of("status", "UP") : Map.of("status", "DOWN"));
        components.put("redis", redisHealthy ? Map.of("status", "UP") : Map.of("status", "DOWN"));
        response.put("components", components);

        if (!isUp) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }

        return ResponseEntity.ok(response);
    }

    private boolean isDatabaseHealthy() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isRedisHealthy() {
        try {
            String pingResponse = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equalsIgnoreCase(pingResponse);
        } catch (Exception e) {
            return false;
        }
    }
}

