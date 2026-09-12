package com.tamvagbackend.controller;

import com.tamvagbackend.dto.ProfileDtos.CustomerProfileResponse;
import com.tamvagbackend.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/customers")
@Tag(name = "Customer Financial Profile", description = "Derived financial identity snapshots, cash flow, debt service ratio, and confidence score")
public class CustomerProfileController {

    private final ProfileService profileService;

    public CustomerProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Retrieve customer profile", description = "Returns aggregated 90-day cash flow, monthly income estimates, debt pressure, savings consistency, and overall confidence score")
    public ResponseEntity<CustomerProfileResponse> getCustomerProfile(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(profileService.getCustomerProfile(id));
    }
}
