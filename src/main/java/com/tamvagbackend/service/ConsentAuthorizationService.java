package com.tamvagbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.domain.entity.Consent;
import com.tamvagbackend.domain.repository.ConsentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ConsentAuthorizationService {

    private final ConsentRepository consentRepository;
    private final ObjectMapper objectMapper;

    public ConsentAuthorizationService(
            ConsentRepository consentRepository,
            ObjectMapper objectMapper
    ) {
        this.consentRepository = consentRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Verifies that the customer has an active, non-expired consent
     * for the requested scope with the specified institution.
     */
    public Consent requireConsent(
            UUID customerId,
            UUID institutionId,
            String requiredScope
    ) {
        Instant now = Instant.now();

        List<Consent> consents = consentRepository.findByCustomerIdAndInstitutionId(
                customerId,
                institutionId
        );

        return consents.stream()
                .filter(consent -> "ACTIVE".equalsIgnoreCase(consent.getStatus()))
                .filter(consent -> consent.getRevokedAt() == null)
                .filter(consent -> consent.getExpiresAt() == null
                        || consent.getExpiresAt().isAfter(now))
                .filter(consent -> hasScope(consent, requiredScope))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Valid customer consent is required for scope: " + requiredScope
                ));
    }

    private boolean hasScope(Consent consent, String requiredScope) {
        if (requiredScope == null || requiredScope.isBlank()) {
            return false;
        }

        List<String> scopes = parseScopes(consent.getScopes());

        return scopes.stream()
                .map(String::trim)
                .anyMatch(scope -> scope.equalsIgnoreCase(requiredScope));
    }

    private List<String> parseScopes(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}