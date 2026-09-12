package com.tamvagbackend.controller;

import com.tamvagbackend.dto.ConsentDtos.ConsentResponse;
import com.tamvagbackend.dto.ConsentDtos.CreateConsentRequest;
import com.tamvagbackend.service.ConsentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/consents")
@Tag(name = "Consent Management", description = "Customer data consent lifecycle, scopes, and revocation")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping
    @Operation(summary = "Create consent", description = "Grant explicit customer consent to an institution for specific data scopes and purpose")
    public ResponseEntity<ConsentResponse> createConsent(@Valid @RequestBody CreateConsentRequest request) {
        ConsentResponse response = consentService.createConsent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get consent", description = "Retrieve consent record details")
    public ResponseEntity<ConsentResponse> getConsent(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(consentService.getConsent(id));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get customer consents", description = "List all consents granted by a customer")
    public ResponseEntity<List<ConsentResponse>> getCustomerConsents(@PathVariable("customerId") UUID customerId) {
        return ResponseEntity.ok(consentService.getCustomerConsents(customerId));
    }

    @PostMapping("/{id}/revoke")
    @Operation(summary = "Revoke consent", description = "Immediately revoke an active consent and create an immutable audit record")
    public ResponseEntity<ConsentResponse> revokeConsent(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(consentService.revokeConsent(id));
    }
}
