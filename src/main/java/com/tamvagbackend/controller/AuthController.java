package com.tamvagbackend.controller;

import com.tamvagbackend.dto.AuthDtos.TokenRequest;
import com.tamvagbackend.dto.AuthDtos.TokenResponse;
import com.tamvagbackend.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@Tag(
        name = "Authentication",
        description = "Partner application authentication and access tokens"
)
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Operation(
        summary = "Obtain access token",
        description = "Authenticates a partner application and returns a JWT access token",
        security = {}
    )
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(
            @Valid @RequestBody TokenRequest request
    ) {

        TokenResponse response =
                authenticationService.authenticate(request);

        return ResponseEntity.ok(response);
    }
}