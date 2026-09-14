package com.tamvagbackend.controller;

import com.tamvagbackend.dto.ApplicationDtos;
import com.tamvagbackend.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/applications")
@Tag(
        name = "Applications",
        description = "TAMVA partner application management"
)
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_application:manage')")
    @Operation(summary = "Create a partner application")
    public ResponseEntity<ApplicationDtos.CreateApplicationResponse> create(
            @Valid @RequestBody ApplicationDtos.CreateApplicationRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(applicationService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_application:read')")
    @Operation(summary = "List partner applications")
    public ResponseEntity<List<ApplicationDtos.ApplicationResponse>> list() {
        return ResponseEntity.ok(applicationService.list());
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAuthority('SCOPE_application:read')")
    @Operation(summary = "Get a partner application")
    public ResponseEntity<ApplicationDtos.ApplicationResponse> get(
            @PathVariable UUID applicationId
    ) {
        return ResponseEntity.ok(
                applicationService.get(applicationId)
        );
    }

    @PatchMapping("/{applicationId}/status")
    @PreAuthorize("hasAuthority('SCOPE_application:manage')")
    @Operation(summary = "Activate or deactivate an application")
    public ResponseEntity<ApplicationDtos.ApplicationResponse> updateStatus(
            @PathVariable UUID applicationId,
            @Valid @RequestBody ApplicationDtos.UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(
                applicationService.updateStatus(applicationId, request)
        );
    }

    @PatchMapping("/{applicationId}/scopes")
    @PreAuthorize("hasAuthority('SCOPE_application:manage')")
    @Operation(summary = "Update application scopes")
    public ResponseEntity<ApplicationDtos.ApplicationResponse> updateScopes(
            @PathVariable UUID applicationId,
            @Valid @RequestBody ApplicationDtos.UpdateScopesRequest request
    ) {
        return ResponseEntity.ok(
                applicationService.updateScopes(applicationId, request)
        );
    }

    @PostMapping("/{applicationId}/rotate-secret")
    @PreAuthorize("hasAuthority('SCOPE_application:manage')")
    @Operation(summary = "Rotate an application client secret")
    public ResponseEntity<ApplicationDtos.RotateSecretResponse> rotateSecret(
            @PathVariable UUID applicationId
    ) {
        return ResponseEntity.ok(
                applicationService.rotateSecret(applicationId)
        );
    }
}
