package com.tamvagbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.domain.entity.Customer;
import com.tamvagbackend.domain.entity.Institution;
import com.tamvagbackend.domain.entity.Passport;
import com.tamvagbackend.domain.entity.PassportShare;
import com.tamvagbackend.domain.repository.CustomerRepository;
import com.tamvagbackend.domain.repository.InstitutionRepository;
import com.tamvagbackend.domain.repository.PassportRepository;
import com.tamvagbackend.domain.repository.PassportShareRepository;
import com.tamvagbackend.dto.PassportDtos.CreatePassportRequest;
import com.tamvagbackend.dto.PassportDtos.CreateShareRequest;
import com.tamvagbackend.dto.PassportDtos.PassportResponse;
import com.tamvagbackend.dto.PassportDtos.PassportShareResponse;
import com.tamvagbackend.dto.ProfileDtos.CashFlowSummary;
import com.tamvagbackend.dto.ProfileDtos.CustomerProfileResponse;
import com.tamvagbackend.dto.ProfileDtos.DebtSummary;
import com.tamvagbackend.dto.ProfileDtos.IncomeSummary;
import com.tamvagbackend.dto.ProfileDtos.SavingsSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PassportService {

    private static final int DEFAULT_PASSPORT_VALIDITY_DAYS = 365;
    private static final int DEFAULT_SHARE_VALIDITY_HOURS = 72;

    private static final Set<String> SUPPORTED_CATEGORIES = Set.of(
            "INCOME",
            "CASH_FLOW",
            "DEBT",
            "REPAYMENT",
            "SAVINGS",
            "BUSINESS_FLOWS"
    );

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
    public PassportResponse createPassport(
            CreatePassportRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Passport request is required"
            );
        }

        Customer customer = customerRepository
                .findById(request.customerId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found: "
                                        + request.customerId()
                        )
                );

        List<String> dataCategories =
                normalizeCategories(request.dataCategories());

        validateCategories(
                dataCategories,
                "data_categories"
        );

        int validityDays =
                request.validityDays() != null
                        && request.validityDays() > 0
                        ? request.validityDays()
                        : DEFAULT_PASSPORT_VALIDITY_DAYS;

        Instant now = Instant.now();
        Instant expiresAt =
                now.plus(Duration.ofDays(validityDays));

        Passport passport = new Passport();

        passport.setCustomer(customer);
        passport.setPassportType(
                request.passportType().trim()
        );
        passport.setPurpose(
                request.purpose().trim()
        );
        passport.setDataCategories(
                toJson(dataCategories)
        );
        passport.setStatus("ACTIVE");
        passport.setVersion("1.0");
        passport.setCreatedAt(now);
        passport.setExpiresAt(expiresAt);

        Passport saved =
                passportRepository.save(passport);

        auditService.logEvent(
                "CUSTOMER",
                customer.getCustomerId().toString(),
                "PASSPORT_CREATED",
                "PASSPORT",
                saved.getPassportId().toString(),
                null,
                buildPassportAuditPayload(saved)
        );

        CustomerProfileResponse profile =
                profileService.getCustomerProfile(
                        customer.getCustomerId()
                );

        return new PassportResponse(
                saved.getPassportId(),
                customer.getCustomerId(),
                saved.getPassportType(),
                saved.getPurpose(),
                dataCategories,
                saved.getStatus(),
                saved.getVersion(),
                saved.getCreatedAt(),
                saved.getExpiresAt(),
                filterProfile(
                        profile,
                        new HashSet<>(dataCategories)
                )
        );
    }

    @Transactional
    public PassportShareResponse createShare(
            UUID passportId,
            CreateShareRequest request
    ) {
        if (passportId == null) {
            throw new IllegalArgumentException(
                    "passport_id is required"
            );
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Share request is required"
            );
        }

        Passport passport = passportRepository
                .findById(passportId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Passport not found: "
                                        + passportId
                        )
                );

        ensurePassportUsable(passport);

        Institution recipient =
                institutionRepository
                        .findById(request.recipientId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Recipient institution not found: "
                                                + request.recipientId()
                                )
                        );

        List<String> passportCategories =
                normalizeCategories(
                        fromJson(passport.getDataCategories())
                );

        validateCategories(
                passportCategories,
                "passport data_categories"
        );

        List<String> scopes =
                normalizeCategories(request.scopes());

        validateCategories(
                scopes,
                "scopes"
        );

        Set<String> allowedCategories =
                new HashSet<>(passportCategories);

        List<String> unauthorizedScopes =
                scopes.stream()
                        .filter(scope ->
                                !allowedCategories.contains(scope)
                        )
                        .toList();

        if (!unauthorizedScopes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Requested scopes are not included in "
                            + "passport data_categories: "
                            + unauthorizedScopes
            );
        }

        int durationHours =
                request.durationHours() != null
                        && request.durationHours() > 0
                        ? request.durationHours()
                        : DEFAULT_SHARE_VALIDITY_HOURS;

        Instant now = Instant.now();

        Instant requestedExpiry =
                now.plus(Duration.ofHours(durationHours));

        /*
         * A share must never outlive the passport itself.
         */
        Instant expiresAt =
                passport.getExpiresAt() != null
                        && passport.getExpiresAt()
                        .isBefore(requestedExpiry)
                        ? passport.getExpiresAt()
                        : requestedExpiry;

        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException(
                    "Share duration exceeds the remaining "
                            + "passport validity"
            );
        }

        String shareToken =
                "SHR_"
                        + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .toUpperCase(Locale.ROOT);

        PassportShare share =
                new PassportShare();

        share.setPassport(passport);
        share.setRecipient(recipient);
        share.setPurpose(
                request.purpose().trim()
        );
        share.setScopes(
                toJson(scopes)
        );
        share.setShareToken(shareToken);
        share.setStatus("ACTIVE");
        share.setSharedAt(now);
        share.setExpiresAt(expiresAt);
        share.setRevokedAt(null);

        PassportShare savedShare =
                passportShareRepository.save(share);

        auditService.logEvent(
                "CUSTOMER",
                passport.getCustomer()
                        .getCustomerId()
                        .toString(),
                "PASSPORT_SHARED",
                "PASSPORT_SHARE",
                savedShare.getShareId().toString(),
                null,
                String.format(
                        "Passport %s shared with institution %s "
                                + "for purpose %s with scopes %s; "
                                + "expires_at=%s",
                        passportId,
                        recipient.getInstitutionId(),
                        savedShare.getPurpose(),
                        scopes,
                        expiresAt
                )
        );

        return toShareResponse(savedShare);
    }

    @Transactional
    public PassportShareResponse revokeShare(
            UUID shareId
    ) {
        if (shareId == null) {
            throw new IllegalArgumentException(
                    "share_id is required"
            );
        }

        PassportShare share =
                passportShareRepository
                        .findById(shareId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Passport share not found: "
                                                + shareId
                                )
                        );

        if ("REVOKED".equalsIgnoreCase(
                share.getStatus()
        )) {
            return toShareResponse(share);
        }

        if ("EXPIRED".equalsIgnoreCase(
                share.getStatus()
        )) {
            throw new IllegalStateException(
                    "Passport share is already expired"
            );
        }

        Instant now = Instant.now();

        share.setStatus("REVOKED");
        share.setRevokedAt(now);

        PassportShare saved =
                passportShareRepository.save(share);

        auditService.logEvent(
                "CUSTOMER",
                share.getPassport()
                        .getCustomer()
                        .getCustomerId()
                        .toString(),
                "PASSPORT_SHARE_REVOKED",
                "PASSPORT_SHARE",
                saved.getShareId().toString(),
                null,
                String.format(
                        "Passport share %s revoked for passport %s",
                        saved.getShareId(),
                        saved.getPassport().getPassportId()
                )
        );

        return toShareResponse(saved);
    }

    @Transactional
    public PassportResponse accessPassportByToken(
            String shareToken
    ) {
        if (shareToken == null || shareToken.isBlank()) {
            throw new IllegalArgumentException(
                    "share_token is required"
            );
        }

        PassportShare share =
                passportShareRepository
                        .findByShareToken(
                                shareToken.trim()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid or expired share token"
                                )
                        );

        Instant now = Instant.now();

        if ("REVOKED".equalsIgnoreCase(
                share.getStatus()
        )) {
            throw new IllegalStateException(
                    "Passport share has been revoked"
            );
        }

        if ("EXPIRED".equalsIgnoreCase(
                share.getStatus()
        )) {
            throw new IllegalStateException(
                    "Passport share has expired"
            );
        }

        if (share.getExpiresAt() != null
                && !share.getExpiresAt().isAfter(now)) {

            share.setStatus("EXPIRED");

            passportShareRepository.save(share);

            auditService.logEvent(
                    "SYSTEM",
                    "system",
                    "PASSPORT_SHARE_EXPIRED",
                    "PASSPORT_SHARE",
                    share.getShareId().toString(),
                    null,
                    String.format(
                            "Passport share %s expired at %s",
                            share.getShareId(),
                            share.getExpiresAt()
                    )
            );

            throw new IllegalStateException(
                    "Passport share has expired"
            );
        }

        Passport passport = share.getPassport();

        ensurePassportUsable(passport);

        List<String> passportCategories =
                normalizeCategories(
                        fromJson(
                                passport.getDataCategories()
                        )
                );

        List<String> scopes =
                normalizeCategories(
                        fromJson(share.getScopes())
                );

        validateCategories(
                passportCategories,
                "passport data_categories"
        );

        validateCategories(
                scopes,
                "share scopes"
        );

        Set<String> allowedCategories =
                new HashSet<>(passportCategories);

        if (!allowedCategories.containsAll(scopes)) {
            throw new IllegalStateException(
                    "Passport share contains scopes that are "
                            + "not authorized by the passport"
            );
        }

        CustomerProfileResponse profile =
                profileService.getCustomerProfile(
                        passport.getCustomer()
                                .getCustomerId()
                );

        CustomerProfileResponse scopedProfile =
                filterProfile(
                        profile,
                        new HashSet<>(scopes)
                );

        auditService.logEvent(
                "INSTITUTION",
                share.getRecipient()
                        .getInstitutionId()
                        .toString(),
                "PASSPORT_ACCESSED",
                "PASSPORT_SHARE",
                share.getShareId().toString(),
                null,
                String.format(
                        "Institution %s accessed passport %s "
                                + "through active share with scopes %s",
                        share.getRecipient()
                                .getInstitutionId(),
                        passport.getPassportId(),
                        scopes
                )
        );

        return new PassportResponse(
                passport.getPassportId(),
                passport.getCustomer().getCustomerId(),
                passport.getPassportType(),
                passport.getPurpose(),
                passportCategories,
                passport.getStatus(),
                passport.getVersion(),
                passport.getCreatedAt(),
                passport.getExpiresAt(),
                scopedProfile
        );
    }

    private CustomerProfileResponse filterProfile(
            CustomerProfileResponse profile,
            Set<String> scopes
    ) {
        if (profile == null) {
            return null;
        }

        boolean income =
                scopes.contains("INCOME");

        boolean cashFlow =
                scopes.contains("CASH_FLOW")
                        || scopes.contains("BUSINESS_FLOWS");

        boolean savings =
                scopes.contains("SAVINGS");

        boolean debt =
                scopes.contains("DEBT")
                        || scopes.contains("REPAYMENT");

        IncomeSummary incomeSummary =
                income
                        ? profile.income()
                        : null;

        CashFlowSummary cashFlowSummary =
                cashFlow
                        ? profile.cashFlow()
                        : null;

        SavingsSummary savingsSummary =
                savings
                        ? profile.savings()
                        : null;

        DebtSummary debtSummary =
                debt
                        ? profile.debt()
                        : null;

        /*
         * Confidence and evidence metadata describe the
         * resulting profile as a whole and are therefore
         * retained. The financial sections themselves are
         * restricted to the granted scopes.
         */
        return new CustomerProfileResponse(
                profile.customerId(),
                profile.asOf(),
                incomeSummary,
                cashFlowSummary,
                savingsSummary,
                debtSummary,
                profile.confidenceScore(),
                profile.evidenceWindowDays(),
                profile.profileVersion(),
                profile.connectedAccountsCount(),
                profile.totalTransactionsAnalyzed()
        );
    }

    private void ensurePassportUsable(
            Passport passport
    ) {
        if (passport == null) {
            throw new IllegalStateException(
                    "Passport does not exist"
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(
                passport.getStatus()
        )) {
            throw new IllegalStateException(
                    "Passport is not active"
            );
        }

        Instant expiresAt =
                passport.getExpiresAt();

        if (expiresAt != null
                && !expiresAt.isAfter(Instant.now())) {

            passport.setStatus("EXPIRED");

            passportRepository.save(passport);

            auditService.logEvent(
                    "SYSTEM",
                    "system",
                    "PASSPORT_EXPIRED",
                    "PASSPORT",
                    passport.getPassportId().toString(),
                    null,
                    String.format(
                            "Passport %s expired at %s",
                            passport.getPassportId(),
                            expiresAt
                    )
            );

            throw new IllegalStateException(
                    "Passport has expired"
            );
        }
    }

    private PassportShareResponse toShareResponse(
            PassportShare share
    ) {
        return new PassportShareResponse(
                share.getShareId(),
                share.getPassport().getPassportId(),
                share.getRecipient().getInstitutionId(),
                share.getRecipient().getName(),
                share.getPurpose(),
                normalizeCategories(
                        fromJson(share.getScopes())
                ),
                share.getShareToken(),
                share.getStatus(),
                share.getSharedAt(),
                share.getExpiresAt(),
                share.getRevokedAt()
        );
    }

    private String buildPassportAuditPayload(
            Passport passport
    ) {
        return String.format(
                "Passport %s created: type=%s, purpose=%s, "
                        + "version=%s, expires_at=%s",
                passport.getPassportId(),
                passport.getPassportType(),
                passport.getPurpose(),
                passport.getVersion(),
                passport.getExpiresAt()
        );
    }

    private List<String> normalizeCategories(
            List<String> values
    ) {
        if (values == null) {
            return Collections.emptyList();
        }

        return values.stream()
                .filter(value -> value != null)
                .map(value ->
                        value.trim()
                                .toUpperCase(Locale.ROOT)
                )
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private void validateCategories(
            List<String> categories,
            String fieldName
    ) {
        List<String> unsupported =
                categories.stream()
                        .filter(category ->
                                !SUPPORTED_CATEGORIES
                                        .contains(category)
                        )
                        .toList();

        if (!unsupported.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName
                            + " contains unsupported categories: "
                            + unsupported
                            + ". Supported categories: "
                            + SUPPORTED_CATEGORIES
            );
        }
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to serialize passport data",
                    exception
            );
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(
                                    List.class,
                                    String.class
                            )
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Invalid stored passport data",
                    exception
            );
        }
    }
}