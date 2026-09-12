package com.tamvagbackend.controller;

import com.tamvagbackend.dto.CaseDtos.CaseActionRequest;
import com.tamvagbackend.dto.CaseDtos.CaseResponse;
import com.tamvagbackend.service.CaseManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/cases")
@Tag(name = "Case Management & Fraud Ops", description = "Operational risk alert queue, case triaging, analyst actions, and dispositions")
public class CaseController {

    private final CaseManagementService caseManagementService;

    public CaseController(CaseManagementService caseManagementService) {
        this.caseManagementService = caseManagementService;
    }

    @GetMapping
    @Operation(summary = "List risk/fraud cases", description = "Retrieve operational cases filtered by status or severity")
    public ResponseEntity<List<CaseResponse>> getCases(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "severity", required = false) String severity
    ) {
        return ResponseEntity.ok(caseManagementService.getCases(status, severity));
    }

    @PostMapping("/{id}/actions")
    @Operation(summary = "Apply case action", description = "Apply analyst action (CHALLENGE, HOLD, RELEASE, ESCALATE, BLOCK) and set disposition")
    public ResponseEntity<CaseResponse> updateCase(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CaseActionRequest request
    ) {
        return ResponseEntity.ok(caseManagementService.updateCase(id, request));
    }
}
