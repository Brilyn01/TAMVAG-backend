package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.CaseRecord;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseRecordRepository
        extends JpaRepository<CaseRecord, UUID> {

    List<CaseRecord> findByStatusAndInstitutionId(
            String status,
            UUID institutionId
    );

    List<CaseRecord> findBySeverityAndInstitutionId(
            String severity,
            UUID institutionId
    );

    List<CaseRecord>
    findTop50ByInstitutionIdOrderByCreatedAtDesc(
            UUID institutionId
    );

    Optional<CaseRecord> findByCaseIdAndInstitutionId(
            UUID caseId,
            UUID institutionId
    );

    List<CaseRecord> findByStatus(String status);

    List<CaseRecord> findBySeverity(String severity);

    List<CaseRecord> findTop50ByOrderByCreatedAtDesc();
}