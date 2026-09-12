package com.tamvagbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.domain.entity.Consent;
import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.Institution;
import com.tamvagbackend.domain.repository.ConsentRepository;
import com.tamvagbackend.domain.repository.CustomerRepository;
import com.tamvagbackend.domain.repository.InstitutionRepository;
import com.tamvagbackend.dto.ConsentDtos.ConsentResponse;
import com.tamvagbackend.dto.ConsentDtos.CreateConsentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConsentService {

    private static final Logger log = LoggerFactory.getLogger(ConsentService.class);

    private final ConsentRepository consentRepository;
    private final CustomerRepository customerRepository;
    private final InstitutionRepository institutionRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public ConsentService(
            ConsentRepository consentRepository,
            CustomerRepository customerRepository,
            InstitutionRepository institutionRepository,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.consentRepository = consentRepository;
        this.customerRepository = customerRepository;
        this.institutionRepository = institutionRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ConsentResponse createConsent(CreateConsentRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        Institution institution = institutionRepository.findById(request.institutionId())
                .orElseThrow(() -> new IllegalArgumentException("Institution not found: " + request.institutionId()));

        int durationDays = request.durationDays() != null && request.durationDays() > 0 ? request.durationDays() : 90;
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofDays(durationDays));

        Consent consent = new Consent();
        consent.setCustomer(customer);
        consent.setInstitution(institution);
        consent.setPurpose(request.purpose());
        consent.setScopes(toJson(request.scopes()));
        consent.setStatus("ACTIVE");
        consent.setGrantedAt(now);
        consent.setExpiresAt(expiresAt);
        consent.setCreatedAt(now);

        Consent saved = consentRepository.save(consent);

        auditService.logEvent(
                "CUSTOMER",
                customer.getCustomerId().toString(),
                "CONSENT_GRANTED",
                "CONSENT",
                saved.getConsentId().toString(),
                null,
                String.format("Granted consent to %s for purpose '%s'", institution.getName(), request.purpose())
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ConsentResponse getConsent(UUID consentId) {
        Consent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new IllegalArgumentException("Consent not found: " + consentId));
        return toResponse(consent);
    }

    @Transactional(readOnly = true)
    public List<ConsentResponse> getCustomerConsents(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        return consentRepository.findByCustomer(customer).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConsentResponse revokeConsent(UUID consentId) {
        Consent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new IllegalArgumentException("Consent not found: " + consentId));

        if ("REVOKED".equalsIgnoreCase(consent.getStatus())) {
            return toResponse(consent);
        }

        consent.setStatus("REVOKED");
        consent.setRevokedAt(Instant.now());
        Consent updated = consentRepository.save(consent);

        auditService.logEvent(
                "CUSTOMER",
                consent.getCustomer().getCustomerId().toString(),
                "CONSENT_REVOKED",
                "CONSENT",
                consentId.toString(),
                null,
                "Customer revoked consent for institution " + consent.getInstitution().getName()
        );

        return toResponse(updated);
    }

    private ConsentResponse toResponse(Consent c) {
        List<String> scopesList = fromJson(c.getScopes());
        return new ConsentResponse(
                c.getConsentId(),
                c.getCustomer().getCustomerId(),
                c.getInstitution().getInstitutionId(),
                c.getInstitution().getName(),
                c.getPurpose(),
                scopesList,
                c.getStatus(),
                c.getGrantedAt(),
                c.getExpiresAt(),
                c.getRevokedAt(),
                c.getCreatedAt()
        );
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
