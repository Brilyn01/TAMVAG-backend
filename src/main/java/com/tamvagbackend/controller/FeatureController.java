
package com.tamvagbackend.controller;

import com.tamvagbackend.domain.entity.FeatureSnapshot;
import com.tamvagbackend.dto.FeatureDtos;
import com.tamvagbackend.service.ConsentAuthorizationService;
import com.tamvagbackend.service.FeatureComputationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/v1/features")
public class FeatureController {

    private final FeatureComputationService featureComputationService;
    private final ConsentAuthorizationService consentAuthorizationService;

    public FeatureController(
            FeatureComputationService featureComputationService,
            ConsentAuthorizationService consentAuthorizationService
    ) {
        this.featureComputationService =
                featureComputationService;

        this.consentAuthorizationService =
                consentAuthorizationService;
    }

    @PostMapping("/compute")
    @PreAuthorize("hasAuthority('SCOPE_risk:evaluate')")
    public FeatureDtos.FeatureSnapshotResponse compute(
            @Valid
            @RequestBody
            FeatureDtos.ComputeFeatureRequest request,
            Authentication authentication
    ) {
        Jwt jwt = extractJwt(authentication);

        String tokenType =
                jwt.getClaimAsString("token_type");

        if ("user".equalsIgnoreCase(tokenType)) {

            UUID authenticatedCustomerId =
                    extractCustomerId(jwt);

            requireCustomerOwnership(
                    request.customerId(),
                    authenticatedCustomerId
            );

            FeatureSnapshot snapshot =
                    featureComputationService.compute(
                            authenticatedCustomerId,
                            request.periodStart(),
                            request.periodEnd()
                    );

            return toResponse(snapshot);
        }

        if ("institution".equalsIgnoreCase(tokenType)) {

            UUID institutionId =
                    extractInstitutionId(jwt);

            /*
             * Feature computation is derived from customer
             * financial data. The institution must have
             * active CASH_FLOW consent for the customer.
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

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Unsupported or missing token type"
        );
    }

    private Jwt extractJwt(Authentication authentication) {

        if (authentication == null
                || authentication.getPrincipal() == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        Object principal =
                authentication.getPrincipal();

        if (principal instanceof Jwt jwt) {
            return jwt;
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid authentication principal"
        );
    }

    private UUID extractCustomerId(Jwt jwt) {

        String customerIdClaim =
                jwt.getClaimAsString("customer_id");

        if (customerIdClaim == null
                || customerIdClaim.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Authenticated user is not linked to a customer profile"
            );
        }

        try {

            return UUID.fromString(customerIdClaim);

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Invalid customer identity in access token"
            );
        }
    }

    private UUID extractInstitutionId(Jwt jwt) {

        String institutionIdClaim =
                jwt.getClaimAsString("institution_id");

        if (institutionIdClaim == null
                || institutionIdClaim.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Institution context is missing"
            );
        }

        try {

            return UUID.fromString(institutionIdClaim);

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid institution identity in access token"
            );
        }
    }

    private void requireCustomerOwnership(
            UUID requestedCustomerId,
            UUID authenticatedCustomerId
    ) {

        if (requestedCustomerId == null
                || authenticatedCustomerId == null
                || !requestedCustomerId.equals(authenticatedCustomerId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can access only your own customer features"
            );
        }
    }

    private FeatureDtos.FeatureSnapshotResponse toResponse(
            FeatureSnapshot snapshot
    ) {

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