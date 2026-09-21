
package com.tamvagbackend.controller;

import com.tamvagbackend.dto.PassportDtos.CreatePassportRequest;
import com.tamvagbackend.dto.PassportDtos.CreateShareRequest;
import com.tamvagbackend.dto.PassportDtos.PassportResponse;
import com.tamvagbackend.dto.PassportDtos.PassportShareResponse;
import com.tamvagbackend.service.PassportService;
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

import java.util.UUID;

@RestController
@RequestMapping("/v1/passports")
@Tag(
        name = "Financial Passport",
        description =
                "Portable, customer-permissioned living financial "
                        + "profile creation and tokenized sharing"
)
public class PassportController {

    private final PassportService passportService;

    public PassportController(
            PassportService passportService
    ) {
        this.passportService = passportService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_profile:write')")
    @Operation(
            summary = "Create passport",
            description =
                    "Create a customer-permissioned financial passport"
    )
    public ResponseEntity<PassportResponse> createPassport(
            @Valid
            @RequestBody
            CreatePassportRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID authenticatedCustomerId =
                extractAuthenticatedCustomerId(jwt);

        PassportResponse response =
                passportService.createPassport(
                        request,
                        authenticatedCustomerId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{id}/shares")
    @PreAuthorize("hasAuthority('SCOPE_profile:write')")
    @Operation(
            summary = "Share passport",
            description =
                    "Generate a scoped, time-limited share token "
                            + "for an institution"
    )
    public ResponseEntity<PassportShareResponse> createShare(
            @PathVariable("id") UUID passportId,
            @Valid
            @RequestBody
            CreateShareRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID authenticatedCustomerId =
                extractAuthenticatedCustomerId(jwt);

        PassportShareResponse response =
                passportService.createShare(
                        passportId,
                        request,
                        authenticatedCustomerId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @DeleteMapping("/shares/{shareId}")
    @PreAuthorize("hasAuthority('SCOPE_profile:write')")
    @Operation(
            summary = "Revoke passport share",
            description =
                    "Permanently revoke an active passport share"
    )
    public ResponseEntity<PassportShareResponse> revokeShare(
            @PathVariable("shareId") UUID shareId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID authenticatedCustomerId =
                extractAuthenticatedCustomerId(jwt);

        return ResponseEntity.ok(
                passportService.revokeShare(
                        shareId,
                        authenticatedCustomerId
                )
        );
    }

    @GetMapping("/shares/{token}")
    @Operation(
            summary = "Access passport via token",
            description =
                    "Retrieve an active, non-expired financial "
                            + "passport using a share token"
    )
    public ResponseEntity<PassportResponse> accessPassportByToken(
            @PathVariable("token") String token
    ) {
        return ResponseEntity.ok(
                passportService.accessPassportByToken(token)
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
                    "Only authenticated customers can manage passports"
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