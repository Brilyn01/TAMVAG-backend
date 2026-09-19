package com.tamvagbackend.controller;

import com.tamvagbackend.dto.AuditDtos.ConnectorSyncRequest;
import com.tamvagbackend.dto.AuditDtos.ConnectorSyncResponse;
import com.tamvagbackend.dto.AuditDtos.ConnectionResponse;
import com.tamvagbackend.service.ConnectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/connectors")
@Tag(
        name = "Connectors",
        description = "Customer financial connection synchronization"
)
public class ConnectorController {

    private final ConnectorService connectorService;

    public ConnectorController(ConnectorService connectorService) {
        this.connectorService = connectorService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_connector:read')")
    @Operation(
        summary = "List connector connections",
        description = "Lists customer financial connections belonging to the authenticated institution"
    )
        public ResponseEntity<java.util.List<ConnectionResponse>> getConnections(
                @AuthenticationPrincipal Jwt jwt
        ) {
        UUID institutionId = UUID.fromString(
                jwt.getClaimAsString("institution_id")
        );

        return ResponseEntity.ok(
                connectorService.getConnections(institutionId)
        );
    }

    @PostMapping("/{id}/sync")
    @PreAuthorize("hasAuthority('SCOPE_connector:sync')")
    @Operation(
            summary = "Synchronize connector",
            description = "Synchronizes an active customer financial connection"
    )
    public ResponseEntity<ConnectorSyncResponse> sync(
            @PathVariable("id") UUID connectionId,
            @Valid @RequestBody ConnectorSyncRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID institutionId = UUID.fromString(
                jwt.getClaimAsString("institution_id")
        );

        return ResponseEntity.ok(
                connectorService.sync(
                        connectionId,
                        request,
                        institutionId
                )
        );
    }
}