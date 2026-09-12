package com.tamvagbackend.controller;

import com.tamvagbackend.dto.PassportDtos.*;
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
@Tag(name = "Financial Passport", description = "Portable, customer-permissioned living financial profile creation and tokenized sharing")
public class PassportController {

    private final PassportService passportService;

    public PassportController(PassportService passportService) {
        this.passportService = passportService;
    }

    @PostMapping
    @Operation(summary = "Create passport", description = "Create a customer-permissioned financial passport (e.g. LENDING_PROFILE, BUSINESS_PROFILE)")
    public ResponseEntity<PassportResponse> createPassport(@Valid @RequestBody CreatePassportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passportService.createPassport(request));
    }

    @PostMapping("/{id}/shares")
    @Operation(summary = "Share passport", description = "Generate a scoped, time-limited share token for an institution to access passport data")
    public ResponseEntity<PassportShareResponse> createShare(
            @PathVariable("id") UUID passportId,
            @Valid @RequestBody CreateShareRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(passportService.createShare(passportId, request));
    }

    @GetMapping("/shares/{token}")
    @Operation(summary = "Access passport via token", description = "Retrieve verified living financial passport using a share token")
    public ResponseEntity<PassportResponse> accessPassportByToken(@PathVariable("token") String token) {
        return ResponseEntity.ok(passportService.accessPassportByToken(token));
    }
}
