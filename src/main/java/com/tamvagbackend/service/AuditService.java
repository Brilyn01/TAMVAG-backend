package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.AuditEvent;
import com.tamvagbackend.domain.repository.AuditEventRepository;
import com.tamvagbackend.dto.AuditDtos.AuditEventResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public AuditEvent logEvent(String actorType, String actorId, String action, String resourceType, String resourceId, String correlationId, String payload) {
        AuditEvent event = new AuditEvent();
        event.setActorType(actorType != null ? actorType : "SYSTEM");
        event.setActorId(actorId != null ? actorId : "system");
        event.setAction(action);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        event.setTimestamp(Instant.now());
        event.setCorrelationId(correlationId != null ? correlationId : UUID.randomUUID().toString());
        event.setPayload(payload);

        // Compute tamper-evident hash chained from previous event
        Optional<AuditEvent> previousEvent = auditEventRepository.findTopByOrderByTimestampDesc();
        String previousHash = previousEvent.map(AuditEvent::getEventHash).orElse("GENESIS_TAMVA_ROOT_BLOCK_2026");

        String dataToHash = previousHash + "|" + event.getTimestamp().toString() + "|" + event.getActorId() + "|" + action + "|" + resourceId + "|" + (payload != null ? payload : "");
        event.setEventHash(sha256(dataToHash));

        AuditEvent saved = auditEventRepository.save(event);
        log.info("[AUDIT] {} on {}/{} by {} (Hash: {})", action, resourceType, resourceId, actorId, saved.getEventHash());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getRecentEvents() {
        return auditEventRepository.findTop50ByOrderByTimestampDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getEventsForResource(String resourceType, String resourceId) {
        return auditEventRepository.findByResourceTypeAndResourceIdOrderByTimestampDesc(resourceType, resourceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AuditEventResponse toResponse(AuditEvent e) {
        return new AuditEventResponse(
                e.getAuditId(),
                e.getActorType(),
                e.getActorId(),
                e.getAction(),
                e.getResourceType(),
                e.getResourceId(),
                e.getTimestamp(),
                e.getCorrelationId(),
                e.getEventHash(),
                e.getPayload()
        );
    }

    private String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("SHA-256 algorithm missing", ex);
        }
    }
}
