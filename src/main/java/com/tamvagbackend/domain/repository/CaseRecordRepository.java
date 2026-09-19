package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.CaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseRecordRepository extends JpaRepository<CaseRecord, UUID> {

    List<CaseRecord> findByStatusAndRiskEvent_Account_Institution_InstitutionId(
            String status,
            UUID institutionId
    );

    List<CaseRecord> findBySeverityAndRiskEvent_Account_Institution_InstitutionId(
            String severity,
            UUID institutionId
    );

    List<CaseRecord> findTop50ByRiskEvent_Account_Institution_InstitutionIdOrderByCreatedAtDesc(
            UUID institutionId
    );

    Optional<CaseRecord> findByCaseIdAndRiskEvent_Account_Institution_InstitutionId(
            UUID caseId,
            UUID institutionId
    );
}
