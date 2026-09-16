package com.tamvagbackend.controller;

import com.tamvagbackend.domain.entity.FeatureSnapshot;
import com.tamvagbackend.dto.FeatureDtos;
import com.tamvagbackend.service.ConsentAuthorizationService;
import com.tamvagbackend.service.FeatureComputationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/features")
public class FeatureController {

    private final FeatureComputationService featureComputationService;
    private final ConsentAuthorizationService consentAuthorizationService;

    public FeatureController(
            FeatureComputationService featureComputationService,
            ConsentAuthorizationService consentAuthorizationService) {
        this.featureComputationService = featureComputationService;
        this.consentAuthorizationService = consentAuthorizationService;
    }

    @PostMapping("/compute")
    @PreAuthorize("hasAuthority('SCOPE_risk:evaluate')")
    public FeatureDtos.FeatureSnapshotResponse compute(
            @Valid @RequestBody FeatureDtos.ComputeFeatureRequest request,
            Authentication authentication) {

        UUID institutionId = extractInstitutionId(authentication);

        /*
         * Feature computation is derived from customer financial data,
         * therefore the caller must have active CASH_FLOW consent.
         */
        consentAuthorizationService.requireConsent(
                request.customerId(),
                institutionId,
                "CASH_FLOW"
        );

        FeatureSnapshot snapshot =
                featureComputationService.compute(
                        request.customerId(),
                        request.periodStart(),
                        request.periodEnd()
                );

        return toResponse(snapshot);
    }

    private UUID extractInstitutionId(Authentication authentication) {

        if (authentication.getPrincipal()
                instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {

            String claim = jwt.getClaimAsString("institution_id");

            if (claim != null && !claim.isBlank()) {
                return UUID.fromString(claim);
            }
        }

        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Institution context is missing"
        );
    }

    private FeatureDtos.FeatureSnapshotResponse toResponse(
            FeatureSnapshot snapshot) {

        return new FeatureDtos.FeatureSnapshotResponse(
                snapshot.getFeatureSnapshotId(),
                snapshot.getCustomer().getCustomerId(),
                snapshot.getPeriodStart(),
                snapshot.getPeriodEnd(),
                snapshot.getFeatureVersion(),
                snapshot.getTotalInflows(),
                snapshot.getTotalOutflows(),
                snapshot.getNetCashFlow(),
                snapshot.getTransactionCount(),
                snapshot.getInflowTransactionCount(),
                snapshot.getOutflowTransactionCount(),
                snapshot.getAverageInflow(),
                snapshot.getAverageOutflow(),
                snapshot.getLargestInflow(),
                snapshot.getLargestOutflow(),
                snapshot.getIncomeConsistency(),
                snapshot.getExpenseConsistency(),
                snapshot.getComputedAt()
        );
    }
}