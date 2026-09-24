package com.tamvagbackend.controller;

import com.tamvagbackend.dto.CaseDtos.CaseActionRequest;
import com.tamvagbackend.dto.CaseDtos.CaseResponse;
import com.tamvagbackend.dto.CaseDtos.CreateManualCaseRequest;
import com.tamvagbackend.service.CaseManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/cases")
@Tag(
        name = "Admin Case Management",
        description = "Platform-wide administrative case review, creation, investigation, and disposition"
)
public class AdminCaseController {

    private final CaseManagementService caseManagementService;

    public AdminCaseController(CaseManagementService caseManagementService) {
        this.caseManagementService = caseManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_cases:read')")
    @Operation(
            summary = "List all platform cases",
            description = "Returns recent cases across institutions with optional status or severity filtering. Requires cases:read authority."
    )
    public ResponseEntity<List<CaseResponse>> getCases(
            @Parameter(description = "Case status filter")
            @RequestParam(required = false) String status,

            @Parameter(description = "Case severity filter")
            @RequestParam(required = false) String severity
    ) {
        return ResponseEntity.ok(caseManagementService.getAdminCases(status, severity));
    }

    @GetMapping("/{caseId}")
    @PreAuthorize("hasAuthority('SCOPE_cases:read')")
    @Operation(
            summary = "Get case details",
            description = "Returns detailed risk case by case ID. Requires cases:read authority."
    )
    public ResponseEntity<CaseResponse> getCase(@PathVariable UUID caseId) {
        return ResponseEntity.ok(caseManagementService.getAdminCase(caseId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_cases:create')")
    @Operation(
            summary = "Create manual risk case",
            description = "Creates a new manual investigation case. Requires cases:create authority."
    )
    public ResponseEntity<CaseResponse> createCase(
            @Valid @RequestBody CreateManualCaseRequest request,
            @RequestParam(required = false) UUID institutionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String actor = jwt != null ? jwt.getSubject() : "SYSTEM";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(caseManagementService.createManualCase(request, institutionId, actor));
    }

    @PatchMapping("/{caseId}")
    @PreAuthorize("hasAuthority('SCOPE_cases:write')")
    @Operation(
            summary = "Apply action to case",
            description = "Applies analyst action (CHALLENGE, HOLD, RELEASE, ESCALATE, BLOCK, INVESTIGATE) to a case. Requires cases:write authority."
    )
    public ResponseEntity<CaseResponse> updateCase(
            @PathVariable UUID caseId,
            @Valid @RequestBody CaseActionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String actor = jwt != null ? jwt.getSubject() : "SYSTEM";
        return ResponseEntity.ok(caseManagementService.updateAdminCase(caseId, actor, request));
    }
}
