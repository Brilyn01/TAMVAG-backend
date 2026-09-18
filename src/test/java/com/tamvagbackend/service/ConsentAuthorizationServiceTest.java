package com.tamvagbackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.domain.entity.Consent;
import com.tamvagbackend.domain.repository.ConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsentAuthorizationServiceTest {

    @Mock
    private ConsentRepository consentRepository;

    private ConsentAuthorizationService authorizationService;

    private UUID customerId;
    private UUID institutionId;

    @BeforeEach
    void setUp() {
        authorizationService = new ConsentAuthorizationService(
                consentRepository,
                new ObjectMapper()
        );

        customerId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
    }

    @Test
    void activeConsentWithRequiredScopeIsAllowed() {
        Consent consent = consent("ACTIVE", "[\"profile:read\"]",
                Instant.now().plusSeconds(3600));

        when(consentRepository.findByCustomerIdAndInstitutionId(
                customerId, institutionId
        )).thenReturn(List.of(consent));

        Consent result = authorizationService.requireConsent(
                customerId,
                institutionId,
                "profile:read"
        );

        assertSame(consent, result);
    }

    @Test
    void missingScopeIsForbidden() {
        Consent consent = consent("ACTIVE", "[\"transaction:write\"]",
                Instant.now().plusSeconds(3600));

        when(consentRepository.findByCustomerIdAndInstitutionId(
                customerId, institutionId
        )).thenReturn(List.of(consent));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authorizationService.requireConsent(
                        customerId,
                        institutionId,
                        "profile:read"
                )
        );

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void revokedConsentIsForbidden() {
        Consent consent = consent("REVOKED", "[\"profile:read\"]",
                Instant.now().plusSeconds(3600));
        consent.setRevokedAt(Instant.now());

        when(consentRepository.findByCustomerIdAndInstitutionId(
                customerId, institutionId
        )).thenReturn(List.of(consent));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authorizationService.requireConsent(
                        customerId,
                        institutionId,
                        "profile:read"
                )
        );

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void expiredConsentIsForbidden() {
        Consent consent = consent("ACTIVE", "[\"profile:read\"]",
                Instant.now().minusSeconds(1));

        when(consentRepository.findByCustomerIdAndInstitutionId(
                customerId, institutionId
        )).thenReturn(List.of(consent));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authorizationService.requireConsent(
                        customerId,
                        institutionId,
                        "profile:read"
                )
        );

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void consentWithNullExpiryIsAllowed() {
        Consent consent = consent("ACTIVE", "[\"profile:read\"]", null);

        when(consentRepository.findByCustomerIdAndInstitutionId(
                customerId, institutionId
        )).thenReturn(List.of(consent));

        Consent result = authorizationService.requireConsent(
                customerId,
                institutionId,
                "profile:read"
        );

        assertSame(consent, result);
    }

    private Consent consent(
            String status,
            String scopes,
            Instant expiresAt
    ) {
        Consent consent = new Consent();
        consent.setCustomer(null);
        consent.setInstitution(null);
        consent.setStatus(status);
        consent.setScopes(scopes);
        consent.setExpiresAt(expiresAt);
        return consent;
    }
}
