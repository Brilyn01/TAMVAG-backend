package com.tamvagbackend.controller;

import com.tamvagbackend.dto.ProfileDtos.CustomerProfileResponse;
import com.tamvagbackend.service.ConsentAuthorizationService;
import com.tamvagbackend.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/customers")
@Tag(
        name = "Customer Financial Profile",
        description = "Derived financial identity snapshots, cash flow, debt service ratio, and confidence score"
)
public class CustomerProfileController {

    private final ProfileService profileService;
    private final ConsentAuthorizationService consentAuthorizationService;

    public CustomerProfileController(
            ProfileService profileService,
            ConsentAuthorizationService consentAuthorizationService
    ) {
        this.profileService = profileService;
        this.consentAuthorizationService = consentAuthorizationService;
    }

    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAuthority('SCOPE_profile:read')")
    @Operation(
            summary = "Retrieve customer profile",
            description = "Returns aggregated 90-day cash flow, monthly income estimates, debt pressure, savings consistency, and overall confidence score"
    )
    public ResponseEntity<CustomerProfileResponse> getCustomerProfile(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID institutionId = UUID.fromString(
                jwt.getClaimAsString("institution_id")
        );

        consentAuthorizationService.requireConsent(
                id,
                institutionId,
                "profile:read"
        );

        return ResponseEntity.ok(profileService.getCustomerProfile(id));
    }
}
