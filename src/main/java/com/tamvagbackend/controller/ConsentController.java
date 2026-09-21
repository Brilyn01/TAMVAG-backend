package com.tamvagbackend.controller;

import com.tamvagbackend.dto.ConsentDtos.ConsentResponse;
import com.tamvagbackend.dto.ConsentDtos.CreateConsentRequest;
import com.tamvagbackend.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/consents")
@Tag(
        name = "Consent Management",
        description = "Customer data consent lifecycle, scopes, and revocation"
)
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_profile:write')")
    @Operation(
            summary = "Create consent",
            description = "Grant explicit customer consent to an institution for specific data scopes and purpose"
    )
    public ResponseEntity<ConsentResponse> createConsent(
            @Valid @RequestBody CreateConsentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID authenticatedCustomerId =
                extractAuthenticatedCustomerId(jwt);

        ConsentResponse response =
                consentService.createConsent(
                        request,
                        authenticatedCustomerId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_profile:read')")
    @Operation(
            summary = "Get consent",
            description = "Retrieve consent record details"
    )
    public ResponseEntity<ConsentResponse> getConsent(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID authenticatedCustomerId =
                extractAuthenticatedCustomerId(jwt);

        return ResponseEntity.ok(
                consentService.getConsent(
                        id,
                        authenticatedCustomerId
                )
        );
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('SCOPE_profile:read')")
    @Operation(
            summary = "Get customer consents",
            description = "List all consents granted by the authenticated customer"
    )
    public ResponseEntity<List<ConsentResponse>> getCustomerConsents(
            @PathVariable("customerId") UUID customerId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID authenticatedCustomerId =
                extractAuthenticatedCustomerId(jwt);

        return ResponseEntity.ok(
                consentService.getCustomerConsents(
                        customerId,
                        authenticatedCustomerId
                )
        );
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('SCOPE_profile:write')")
    @Operation(
            summary = "Revoke consent",
            description = "Immediately revoke an active consent and create an immutable audit record"
    )
    public ResponseEntity<ConsentResponse> revokeConsent(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID authenticatedCustomerId =
                extractAuthenticatedCustomerId(jwt);

        return ResponseEntity.ok(
                consentService.revokeConsent(
                        id,
                        authenticatedCustomerId
                )
        );
    }

    private UUID extractAuthenticatedCustomerId(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        String tokenType =
                jwt.getClaimAsString("token_type");

        if (!"user".equalsIgnoreCase(tokenType)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only authenticated customers can manage consent"
            );
        }

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
}