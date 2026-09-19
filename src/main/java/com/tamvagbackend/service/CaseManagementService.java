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
        if (institutionId == null) {
            throw new IllegalArgumentException("institution_id is required");
        }
        List<CaseRecord> records;

        if (status != null && !status.isBlank()) {
            String normalizedStatus = normalize(status);
            validateStatus(normalizedStatus);
            records = caseRecordRepository.findByStatusAndRiskEvent_Account_Institution_InstitutionId(normalizedStatus, institutionId);

        } else if (severity != null && !severity.isBlank()) {
            String normalizedSeverity = normalize(severity);
            validateSeverity(normalizedSeverity);
            records = caseRecordRepository.findBySeverityAndRiskEvent_Account_Institution_InstitutionId(normalizedSeverity, institutionId);

        } else {
            records = caseRecordRepository
                    .findTop50ByRiskEvent_Account_Institution_InstitutionIdOrderByCreatedAtDesc(
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
        if (caseId == null) {
            throw new IllegalArgumentException("case_id is required");
        }

        if (institutionId == null) {
            throw new IllegalArgumentException("institution_id is required");
        }

        CaseRecord record = caseRecordRepository
                .findByCaseIdAndRiskEvent_Account_Institution_InstitutionId(
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
            CaseActionRequest request
    ) {
        if (caseId == null) {
            throw new IllegalArgumentException("case_id is required");
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Case action request is required"
            );
        }

        if (institutionId == null) {
            throw new IllegalArgumentException("institution_id is required");
        }

        CaseRecord record = caseRecordRepository
                .findByCaseIdAndRiskEvent_Account_Institution_InstitutionId(
                        caseId,
                        institutionId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Case not found: " + caseId
                        )
                );

        String action = normalize(request.action());

        String previousStatus = normalize(record.getStatus());

        String newStatus = resolveStatusForAction(action);

        applyAssignee(record, request.assignee());
        appendNotes(record, action, request.notes());
        applyDisposition(record, request.disposition());

        record.setStatus(newStatus);
        record.setUpdatedAt(Instant.now());

        CaseRecord updated = caseRecordRepository.save(record);

        auditService.logEvent(
                "ANALYST",
                resolveAuditActor(request.assignee()),
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

    private String resolveAuditActor(String assignee) {
        if (assignee == null || assignee.isBlank()) {
            return "analyst";
        }

        return assignee.trim();
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

        if (record.getRiskEvent() == null) {
            throw new IllegalStateException(
                    "Case " + record.getCaseId()
                            + " has no associated risk event"
            );
        }

        if (record.getRiskEvent().getCustomer() == null) {
            throw new IllegalStateException(
                    "Risk event "
                            + record.getRiskEvent().getRiskEventId()
                            + " has no associated customer"
            );
        }

        return new CaseResponse(
                record.getCaseId(),
                record.getRiskEvent().getRiskEventId(),
                record.getRiskEvent().getCustomer().getCustomerId(),
                record.getRiskEvent().getRiskScore(),
                record.getRiskEvent().getDecision(),
                record.getSeverity(),
                record.getStatus(),
                record.getSource(),
                record.getAssignee(),
                record.getDisposition(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}