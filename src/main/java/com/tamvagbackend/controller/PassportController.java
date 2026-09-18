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
import org.springframework.web.bind.annotation.*;

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
    @Operation(
            summary = "Create passport",
            description =
                    "Create a customer-permissioned financial "
                            + "passport"
    )
    public ResponseEntity<PassportResponse> createPassport(
            @Valid
            @RequestBody
            CreatePassportRequest request
    ) {
        PassportResponse response =
                passportService.createPassport(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{id}/shares")
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
            CreateShareRequest request
    ) {
        PassportShareResponse response =
                passportService.createShare(
                        passportId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @DeleteMapping("/shares/{shareId}")
    @Operation(
            summary = "Revoke passport share",
            description =
                    "Permanently revoke an active passport share"
    )
    public ResponseEntity<PassportShareResponse> revokeShare(
            @PathVariable("shareId") UUID shareId
    ) {
        return ResponseEntity.ok(
                passportService.revokeShare(shareId)
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
}