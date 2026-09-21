package com.tamvagbackend.controller;

import com.tamvagbackend.dto.ProfileDtos.CustomerProfileResponse;
import com.tamvagbackend.service.ConsentAuthorizationService;
import com.tamvagbackend.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
        if (jwt == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        String tokenType = jwt.getClaimAsString("token_type");

        /*
          USER AUTHENTICATION FLOW
         
          A user JWT contains customer_id rather than
          institution_id.
         
          The user can access only the customer profile
          linked to their own authenticated identity.
         */
        if ("user".equalsIgnoreCase(tokenType)) {
            String customerIdClaim =
                    jwt.getClaimAsString("customer_id");

            if (customerIdClaim == null
                    || customerIdClaim.isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Authenticated user is not linked to a customer profile"
                );
            }

            UUID authenticatedCustomerId;

            try {
                authenticatedCustomerId =
                        UUID.fromString(customerIdClaim);

            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Invalid customer identity in access token"
                );
            }

            /*
              Prevent a user from requesting another customer's
              financial profile by changing the path ID.
             */
            if (!authenticatedCustomerId.equals(id)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can access only your own customer profile"
                );
            }

            return ResponseEntity.ok(
                    profileService.getCustomerProfile(
                            authenticatedCustomerId
                    )
            );
        }

        /*
          PARTNER APPLICATION AUTHENTICATION FLOW
         
          Preserve the existing institution-based consent
          authorization for application tokens.
         */
        String institutionIdClaim =
                jwt.getClaimAsString("institution_id");

        if (institutionIdClaim == null
                || institutionIdClaim.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Institution identity is required for application access"
            );
        }

        UUID institutionId;

        try {
            institutionId = UUID.fromString(
                    institutionIdClaim
            );

        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Invalid institution identity in access token"
            );
        }

        consentAuthorizationService.requireConsent(
                id,
                institutionId,
                "profile:read"
        );

        return ResponseEntity.ok(
                profileService.getCustomerProfile(id)
        );
    }
}