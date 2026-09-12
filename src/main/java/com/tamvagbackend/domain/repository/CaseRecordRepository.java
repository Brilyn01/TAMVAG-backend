package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.CaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseRecordRepository extends JpaRepository<CaseRecord, UUID> {
    List<CaseRecord> findByStatus(String status);
    List<CaseRecord> findBySeverity(String severity);
    List<CaseRecord> findTop50ByOrderByCreatedAtDesc();
}
