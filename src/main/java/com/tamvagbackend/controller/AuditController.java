package com.tamvagbackend.controller;

import com.tamvagbackend.dto.AuditDtos.AuditEventResponse;
import com.tamvagbackend.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/audit")
@Tag(name = "Audit & Compliance", description = "Query tamper-evident, SHA-256 hash-chained audit events")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/events")
    @Operation(summary = "Query audit events", description = "Retrieve cryptographic tamper-evident audit records")
    public ResponseEntity<List<AuditEventResponse>> getAuditEvents(
            @RequestParam(name = "resourceType", required = false) String resourceType,
            @RequestParam(name = "resourceId", required = false) String resourceId
    ) {
        if (resourceType != null && resourceId != null) {
            return ResponseEntity.ok(auditService.getEventsForResource(resourceType, resourceId));
        }
        return ResponseEntity.ok(auditService.getRecentEvents());
    }
}
