package com.tamvagbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.domain.entity.*;
import com.tamvagbackend.domain.repository.*;
import com.tamvagbackend.dto.PassportDtos.*;
import com.tamvagbackend.dto.ProfileDtos.CustomerProfileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PassportService {

    private static final Logger log = LoggerFactory.getLogger(PassportService.class);

    private final PassportRepository passportRepository;
    private final PassportShareRepository passportShareRepository;
    private final CustomerRepository customerRepository;
    private final InstitutionRepository institutionRepository;
    private final ProfileService profileService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public PassportService(
            PassportRepository passportRepository,
            PassportShareRepository passportShareRepository,
            CustomerRepository customerRepository,
            InstitutionRepository institutionRepository,
            ProfileService profileService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.passportRepository = passportRepository;
        this.passportShareRepository = passportShareRepository;
        this.customerRepository = customerRepository;
        this.institutionRepository = institutionRepository;
        this.profileService = profileService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PassportResponse createPassport(CreatePassportRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        int days = request.validityDays() != null && request.validityDays() > 0 ? request.validityDays() : 365;
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofDays(days));

        Passport passport = new Passport();
        passport.setCustomer(customer);
        passport.setPassportType(request.passportType());
        passport.setPurpose(request.purpose());
        passport.setDataCategories(toJson(request.dataCategories()));
        passport.setStatus("ACTIVE");
        passport.setVersion("1.0");
        passport.setCreatedAt(now);
        passport.setExpiresAt(expiresAt);

        Passport saved = passportRepository.save(passport);

        auditService.logEvent(
                "CUSTOMER",
                customer.getCustomerId().toString(),
                "PASSPORT_CREATED",
                "PASSPORT",
                saved.getPassportId().toString(),
                null,
                String.format("Created Financial Passport (%s) for purpose: %s", request.passportType(), request.purpose())
        );

        CustomerProfileResponse profile = profileService.getCustomerProfile(customer.getCustomerId());

        return new PassportResponse(
                saved.getPassportId(),
                customer.getCustomerId(),
                saved.getPassportType(),
                saved.getPurpose(),
                request.dataCategories(),
                saved.getStatus(),
                saved.getVersion(),
                saved.getCreatedAt(),
                saved.getExpiresAt(),
                profile
        );
    }

    @Transactional
    public PassportShareResponse createShare(UUID passportId, CreateShareRequest request) {
        Passport passport = passportRepository.findById(passportId)
                .orElseThrow(() -> new IllegalArgumentException("Passport not found: " + passportId));

        Institution recipient = institutionRepository.findById(request.recipientId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient institution not found: " + request.recipientId()));

        int hours = request.durationHours() != null && request.durationHours() > 0 ? request.durationHours() : 72;
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofHours(hours));

        String shareToken = "SHR_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();

        PassportShare share = new PassportShare();
        share.setPassport(passport);
        share.setRecipient(recipient);
        share.setPurpose(request.purpose());
        share.setScopes(toJson(request.scopes()));
        share.setShareToken(shareToken);
        share.setStatus("ACTIVE");
        share.setSharedAt(now);
        share.setExpiresAt(expiresAt);

        PassportShare savedShare = passportShareRepository.save(share);

        auditService.logEvent(
                "CUSTOMER",
                passport.getCustomer().getCustomerId().toString(),
                "PASSPORT_SHARED",
                "PASSPORT_SHARE",
                savedShare.getShareId().toString(),
                null,
                String.format("Shared Passport %s with %s via token %s", passportId, recipient.getName(), shareToken)
        );

        return toShareResponse(savedShare);
    }

    @Transactional(readOnly = true)
    public PassportResponse accessPassportByToken(String shareToken) {
        PassportShare share = passportShareRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired share token: " + shareToken));

        if (!"ACTIVE".equalsIgnoreCase(share.getStatus()) || (share.getExpiresAt() != null && share.getExpiresAt().isBefore(Instant.now()))) {
            throw new IllegalStateException("Passport share is expired or revoked");
        }

        Passport passport = share.getPassport();
        CustomerProfileResponse profile = profileService.getCustomerProfile(passport.getCustomer().getCustomerId());

        auditService.logEvent(
                "INSTITUTION",
                share.getRecipient().getInstitutionId().toString(),
                "PASSPORT_ACCESSED",
                "PASSPORT_SHARE",
                share.getShareId().toString(),
                null,
                "Recipient " + share.getRecipient().getName() + " accessed Financial Passport via token"
        );

        return new PassportResponse(
                passport.getPassportId(),
                passport.getCustomer().getCustomerId(),
                passport.getPassportType(),
                passport.getPurpose(),
                fromJson(passport.getDataCategories()),
                passport.getStatus(),
                passport.getVersion(),
                passport.getCreatedAt(),
                passport.getExpiresAt(),
                profile
        );
    }

    private PassportShareResponse toShareResponse(PassportShare s) {
        return new PassportShareResponse(
                s.getShareId(),
                s.getPassport().getPassportId(),
                s.getRecipient().getInstitutionId(),
                s.getRecipient().getName(),
                s.getPurpose(),
                fromJson(s.getScopes()),
                s.getShareToken(),
                s.getStatus(),
                s.getSharedAt(),
                s.getExpiresAt(),
                s.getRevokedAt()
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
