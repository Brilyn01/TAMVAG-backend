package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.CaseRecord;
import com.tamvagbackend.domain.repository.CaseRecordRepository;
import com.tamvagbackend.dto.CaseDtos.CaseActionRequest;
import com.tamvagbackend.dto.CaseDtos.CaseResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CaseManagementService {

    private final CaseRecordRepository caseRecordRepository;
    private final AuditService auditService;

    public CaseManagementService(
            CaseRecordRepository caseRecordRepository,
            AuditService auditService
    ) {
        this.caseRecordRepository = caseRecordRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> getCases(
            String status,
            String severity,
            UUID institutionId
    ) {
        requireInstitutionId(institutionId);

        List<CaseRecord> records;

        if (status != null && !status.isBlank()) {
            String normalizedStatus = normalize(status);
            validateStatus(normalizedStatus);

            records = caseRecordRepository
                    .findByStatusAndInstitutionId(
                            normalizedStatus,
                            institutionId
                    );

        } else if (severity != null && !severity.isBlank()) {
            String normalizedSeverity = normalize(severity);
            validateSeverity(normalizedSeverity);

            records = caseRecordRepository
                    .findBySeverityAndInstitutionId(
                            normalizedSeverity,
                            institutionId
                    );

        } else {
            records = caseRecordRepository
                    .findTop50ByInstitutionIdOrderByCreatedAtDesc(
                            institutionId
                    );
        }

        return records.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CaseResponse getCase(
            UUID caseId,
            UUID institutionId
    ) {
        requireCaseId(caseId);
        requireInstitutionId(institutionId);

        CaseRecord record = caseRecordRepository
                .findByCaseIdAndInstitutionId(
                        caseId,
                        institutionId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Case not found: " + caseId
                        )
                );

        return toResponse(record);
    }

    @Transactional
    public CaseResponse updateCase(
            UUID caseId,
            UUID institutionId,
            String authenticatedActor,
            CaseActionRequest request
    ) {
        requireCaseId(caseId);
        requireInstitutionId(institutionId);
        requireAuthenticatedActor(authenticatedActor);

        if (request == null) {
            throw new IllegalArgumentException(
                    "Case action request is required"
            );
        }

        CaseRecord record = caseRecordRepository
                .findByCaseIdAndInstitutionId(
                        caseId,
                        institutionId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Case not found: " + caseId
                        )
                );

        String action = normalize(request.action());

        validateAction(action);

        String previousStatus = normalize(record.getStatus());

        String newStatus = resolveStatusForAction(action);

        applyAssignee(
                record,
                request.assignee()
        );

        appendNotes(
                record,
                action,
                request.notes()
        );

        applyDisposition(
                record,
                request.disposition()
        );

        record.setStatus(newStatus);
        record.setUpdatedAt(Instant.now());

        CaseRecord updated = caseRecordRepository.save(record);

        auditService.logEvent(
                "ANALYST",
                authenticatedActor,
                "CASE_ACTION_APPLIED",
                "CASE_RECORD",
                caseId.toString(),
                null,
                buildAuditPayload(
                        caseId,
                        action,
                        previousStatus,
                        updated.getStatus(),
                        updated.getDisposition(),
                        updated.getAssignee()
                )
        );

        return toResponse(updated);
    }

    private void requireCaseId(UUID caseId) {
        if (caseId == null) {
            throw new IllegalArgumentException(
                    "case_id is required"
            );
        }
    }

    private void requireAuthenticatedActor(
        String authenticatedActor
    ) {
        if (authenticatedActor == null
                || authenticatedActor.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated actor is required"
            );
        }
    }

    private void requireInstitutionId(UUID institutionId) {
        if (institutionId == null) {
            throw new IllegalArgumentException(
                    "institution_id is required"
            );
        }
    }

    private void validateAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException(
                    "Case action is required"
            );
        }

        switch (action) {
            case "CHALLENGE",
                 "HOLD",
                 "RELEASE",
                 "ESCALATE",
                 "BLOCK",
                 "INVESTIGATE" -> {
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported case action: " + action
            );
        }
    }

    private String resolveStatusForAction(String action) {
        return switch (action) {
            case "CHALLENGE" -> "TRIAGED";
            case "HOLD" -> "ACTIONED";
            case "RELEASE" -> "RESOLVED";
            case "ESCALATE" -> "ESCALATED";
            case "BLOCK" -> "ACTIONED";
            case "INVESTIGATE" -> "INVESTIGATING";

            default -> throw new IllegalArgumentException(
                    "Unsupported case action: " + action
            );
        };
    }

    private void applyAssignee(
            CaseRecord record,
            String requestedAssignee
    ) {
        if (requestedAssignee == null) {
            return;
        }

        String assignee = requestedAssignee.trim();

        if (!assignee.isBlank()) {
            record.setAssignee(assignee);
        }
    }

    private void appendNotes(
            CaseRecord record,
            String action,
            String requestedNotes
    ) {
        if (requestedNotes == null || requestedNotes.isBlank()) {
            return;
        }

        String newNotes = requestedNotes.trim();
        String existingNotes = record.getNotes();

        String actionEntry =
                "[" + Instant.now() + "] "
                        + action
                        + ": "
                        + newNotes;

        if (existingNotes == null || existingNotes.isBlank()) {
            record.setNotes(actionEntry);
        } else {
            record.setNotes(
                    existingNotes.trim()
                            + "\n"
                            + actionEntry
            );
        }
    }

    private void applyDisposition(
            CaseRecord record,
            String requestedDisposition
    ) {
        if (requestedDisposition == null
                || requestedDisposition.isBlank()) {
            return;
        }

        record.setDisposition(
                normalize(requestedDisposition)
        );
    }
    
    

    private String buildAuditPayload(
            UUID caseId,
            String action,
            String previousStatus,
            String newStatus,
            String disposition,
            String assignee
    ) {
        return String.format(
                "Case %s: action=%s, status=%s->%s, disposition=%s, assignee=%s",
                caseId,
                action,
                previousStatus,
                newStatus,
                disposition,
                assignee
        );
    }

    private void validateStatus(String status) {
        switch (status) {
            case "OPEN",
                 "TRIAGED",
                 "INVESTIGATING",
                 "ACTIONED",
                 "RESOLVED",
                 "ESCALATED" -> {
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported case status: " + status
            );
        }
    }

    private void validateSeverity(String severity) {
        switch (severity) {
            case "LOW",
                 "MEDIUM",
                 "HIGH",
                 "CRITICAL" -> {
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported case severity: " + severity
            );
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        return value.trim()
                .toUpperCase(Locale.ROOT);
    }

    private CaseResponse toResponse(CaseRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(
                    "Case record cannot be null"
            );
        }

        UUID riskEventId = null;
        UUID customerId = record.getCustomerId();
        Integer riskScore = null;
        String riskDecision = null;

        if (record.getRiskEvent() != null) {
            riskEventId = record.getRiskEvent().getRiskEventId();

            if (record.getRiskEvent().getCustomer() != null) {
                customerId = record.getRiskEvent()
                        .getCustomer()
                        .getCustomerId();
            }

            riskScore = record.getRiskEvent().getRiskScore();
            riskDecision = record.getRiskEvent().getDecision();
        }

        return new CaseResponse(
            record.getCaseId(),
            record.getCaseType(),
            record.getInstitutionId(),
            riskEventId,
            customerId,
            riskScore,
            riskDecision,
            record.getTitle(),
            record.getDescription(),
            record.getSeverity(),
            record.getPriority(),
            record.getStatus(),
            record.getSource(),
            record.getAssignee(),
            record.getCreatedBy(),
            record.getDisposition(),
            record.getResolution(),
            record.getNotes(),
            record.getCreatedAt(),
            record.getUpdatedAt(),
            record.getResolvedAt()
        );
    }
}