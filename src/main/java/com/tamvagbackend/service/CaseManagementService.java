package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.CaseRecord;
import com.tamvagbackend.domain.repository.CaseRecordRepository;
import com.tamvagbackend.dto.CaseDtos.CaseActionRequest;
import com.tamvagbackend.dto.CaseDtos.CaseResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CaseManagementService {

    private final CaseRecordRepository caseRecordRepository;
    private final AuditService auditService;

    public CaseManagementService(CaseRecordRepository caseRecordRepository, AuditService auditService) {
        this.caseRecordRepository = caseRecordRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> getCases(String status, String severity) {
        List<CaseRecord> records;
        if (status != null && !status.isBlank()) {
            records = caseRecordRepository.findByStatus(status.toUpperCase());
        } else if (severity != null && !severity.isBlank()) {
            records = caseRecordRepository.findBySeverity(severity.toUpperCase());
        } else {
            records = caseRecordRepository.findTop50ByOrderByCreatedAtDesc();
        }

        return records.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public CaseResponse updateCase(UUID caseId, CaseActionRequest request) {
        CaseRecord record = caseRecordRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));

        if (request.assignee() != null && !request.assignee().isBlank()) {
            record.setAssignee(request.assignee());
        }
        if (request.notes() != null && !request.notes().isBlank()) {
            record.setNotes((record.getNotes() != null ? record.getNotes() + "\n" : "") + request.notes());
        }

        String action = request.action().toUpperCase();
        switch (action) {
            case "RELEASE" -> record.setStatus("RESOLVED");
            case "BLOCK" -> record.setStatus("ACTIONED");
            case "ESCALATE" -> record.setStatus("ESCALATED");
            case "INVESTIGATE" -> record.setStatus("INVESTIGATING");
            default -> record.setStatus("TRIAGED");
        }

        if (request.disposition() != null && !request.disposition().isBlank()) {
            record.setDisposition(request.disposition().toUpperCase());
        }

        record.setUpdatedAt(Instant.now());
        CaseRecord updated = caseRecordRepository.save(record);

        auditService.logEvent(
                "ANALYST",
                request.assignee() != null ? request.assignee() : "analyst",
                "CASE_ACTION_APPLIED",
                "CASE_RECORD",
                caseId.toString(),
                null,
                String.format("Action %s applied to case %s. Status: %s, Disposition: %s", action, caseId, updated.getStatus(), updated.getDisposition())
        );

        return toResponse(updated);
    }

    private CaseResponse toResponse(CaseRecord c) {
        return new CaseResponse(
                c.getCaseId(),
                c.getRiskEvent().getRiskEventId(),
                c.getRiskEvent().getCustomer().getCustomerId(),
                c.getRiskEvent().getRiskScore(),
                c.getRiskEvent().getDecision(),
                c.getSeverity(),
                c.getStatus(),
                c.getSource(),
                c.getAssignee(),
                c.getDisposition(),
                c.getNotes(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
