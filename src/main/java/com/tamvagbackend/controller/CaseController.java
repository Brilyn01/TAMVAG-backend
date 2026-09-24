package com.tamvagbackend.controller;

import com.tamvagbackend.dto.CaseDtos.CaseActionRequest;
import com.tamvagbackend.dto.CaseDtos.CaseResponse;
import com.tamvagbackend.service.CaseManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/cases")
@Tag(
        name = "Case Management",
        description = "Risk case review, investigation, escalation, and disposition"
)
public class CaseController {

    private final CaseManagementService caseManagementService;

    public CaseController(
            CaseManagementService caseManagementService
    ) {
        this.caseManagementService = caseManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_cases:read')")
    @Operation(
            summary = "List risk cases",
            description = "Returns recent cases with optional status or severity filtering"
    )
    public ResponseEntity<List<CaseResponse>> getCases(
            @Parameter(description = "Case status filter")
            @RequestParam(required = false) String status,

            @Parameter(description = "Case severity filter")
            @RequestParam(required = false) String severity,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                caseManagementService.getCases(
                        status,
                        severity,
                        institutionId(jwt)
                )
        );
    }

    @GetMapping("/{caseId}")
    @PreAuthorize("hasAuthority('SCOPE_cases:read')")
    @Operation(
            summary = "Get a risk case",
            description = "Returns a single risk case by case ID"
    )
    public ResponseEntity<CaseResponse> getCase(
            @PathVariable UUID caseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                caseManagementService.getCase(
                        caseId,
                        institutionId(jwt)
                )
        );
    }

    @PatchMapping("/{caseId}")
    @PreAuthorize("hasAuthority('SCOPE_cases:write')")
    @Operation(
            summary = "Apply a case action",
            description = """
                    Applies an analyst action to a risk case.

                    Supported actions:
                    CHALLENGE, HOLD, RELEASE, ESCALATE, BLOCK, INVESTIGATE.

                    The action determines the resulting case status.
                    """
    )
    public ResponseEntity<CaseResponse> updateCase(
            @PathVariable UUID caseId,
            @Valid @RequestBody CaseActionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                caseManagementService.updateCase(
                        caseId,
                        institutionId(jwt),
                        jwt.getSubject(),
                        request
                )
        );
    }

    private UUID institutionId(Jwt jwt) {
        if (jwt == null) {
                throw new AccessDeniedException(
                        "Authenticated token is required"
                );
        }

        String tokenType = jwt.getClaimAsString("token_type");

        if (!"institution".equals(tokenType)) {
                throw new AccessDeniedException(
                        "Institution token is required for case operations"
                );
        }

        String institutionId = jwt.getClaimAsString("institution_id");

        if (institutionId == null || institutionId.isBlank()) {
                throw new AccessDeniedException(
                        "institution_id claim is required"
                );
        }

        try {
                return UUID.fromString(institutionId);
        } catch (IllegalArgumentException exception) {
                throw new AccessDeniedException(
                        "institution_id claim must be a valid UUID",
                        exception
                );
        }
    }
}