package com.tamvagbackend.domain.repository;

import com.tamvagbackend.domain.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findTop50ByOrderByTimestampDesc();
    List<AuditEvent> findByResourceTypeAndResourceIdOrderByTimestampDesc(String resourceType, String resourceId);
    Optional<AuditEvent> findTopByOrderByTimestampDesc();
}
