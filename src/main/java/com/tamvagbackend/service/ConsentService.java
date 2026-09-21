
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConsentService {

    private static final Logger log =
            LoggerFactory.getLogger(ConsentService.class);

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
    public ConsentResponse createConsent(
            CreateConsentRequest request,
            UUID authenticatedCustomerId
    ) {
        requireCustomerOwnership(
                request.customerId(),
                authenticatedCustomerId
        );

        Customer customer =
                customerRepository.findById(authenticatedCustomerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Customer not found"
                                )
                        );

        Institution institution =
                institutionRepository.findById(request.institutionId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Institution not found"
                                )
                        );

        int durationDays =
                request.durationDays() != null
                        && request.durationDays() > 0
                        ? request.durationDays()
                        : 90;

        Instant now = Instant.now();

        Instant expiresAt =
                now.plus(Duration.ofDays(durationDays));

        Consent consent = new Consent();

        consent.setCustomer(customer);
        consent.setInstitution(institution);
        consent.setPurpose(request.purpose());
        consent.setScopes(toJson(request.scopes()));
        consent.setStatus("ACTIVE");
        consent.setGrantedAt(now);
        consent.setExpiresAt(expiresAt);
        consent.setCreatedAt(now);

        Consent saved =
                consentRepository.save(consent);

        auditService.logEvent(
                "CUSTOMER",
                customer.getCustomerId().toString(),
                "CONSENT_GRANTED",
                "CONSENT",
                saved.getConsentId().toString(),
                null,
                String.format(
                        "Granted consent to %s for purpose '%s'",
                        institution.getName(),
                        request.purpose()
                )
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ConsentResponse getConsent(
            UUID consentId,
            UUID authenticatedCustomerId
    ) {
        Consent consent =
                consentRepository.findById(consentId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Consent not found"
                                )
                        );

        requireCustomerOwnership(
                consent.getCustomer().getCustomerId(),
                authenticatedCustomerId
        );

        return toResponse(consent);
    }

    @Transactional(readOnly = true)
    public List<ConsentResponse> getCustomerConsents(
            UUID customerId,
            UUID authenticatedCustomerId
    ) {
        requireCustomerOwnership(
                customerId,
                authenticatedCustomerId
        );

        Customer customer =
                customerRepository.findById(authenticatedCustomerId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Customer not found"
                                )
                        );

        return consentRepository.findByCustomer(customer)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ConsentResponse revokeConsent(
            UUID consentId,
            UUID authenticatedCustomerId
    ) {
        Consent consent =
                consentRepository.findById(consentId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Consent not found"
                                )
                        );

        requireCustomerOwnership(
                consent.getCustomer().getCustomerId(),
                authenticatedCustomerId
        );

        if ("REVOKED".equalsIgnoreCase(consent.getStatus())) {
            return toResponse(consent);
        }

        consent.setStatus("REVOKED");
        consent.setRevokedAt(Instant.now());

        Consent updated =
                consentRepository.save(consent);

        auditService.logEvent(
                "CUSTOMER",
                consent.getCustomer().getCustomerId().toString(),
                "CONSENT_REVOKED",
                "CONSENT",
                consentId.toString(),
                null,
                "Customer revoked consent for institution "
                        + consent.getInstitution().getName()
        );

        return toResponse(updated);
    }

    /**
     * Ensures that the authenticated customer owns
     * the customer record being accessed.
     *
     * This prevents one customer from creating,
     * viewing, listing, or revoking another customer's consent.
     */
    private void requireCustomerOwnership(
            UUID resourceCustomerId,
            UUID authenticatedCustomerId
    ) {
        if (resourceCustomerId == null
                || authenticatedCustomerId == null
                || !resourceCustomerId.equals(authenticatedCustomerId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can access only your own customer consent"
            );
        }
    }

    private ConsentResponse toResponse(Consent c) {
        List<String> scopesList =
                fromJson(c.getScopes());

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
            log.error(
                    "Failed to serialize consent scopes",
                    e
            );

            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(json, List.class);

        } catch (Exception e) {
            log.error(
                    "Failed to deserialize consent scopes",
                    e
            );

            return Collections.emptyList();
        }
    }
}