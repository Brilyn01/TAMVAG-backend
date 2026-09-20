package com.tamvagbackend.controller;

import com.tamvagbackend.dto.UserAuthDtos.*;
import com.tamvagbackend.service.UserAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
@Tag(
        name = "User Authentication & Profile",
        description = "End-user registration, authentication, token refresh, and profile management"
)
public class UserController {

    private final UserAuthService userAuthService;

    public UserController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping("/signup")
    @Operation(
            summary = "Register a new user",
            description = "Creates a login identity and associated customer profile",
            security = {}
    )
    public ResponseEntity<SignUpResponse> signup(@Valid @RequestBody SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userAuthService.signUp(request));
    }

    @PostMapping("/signin")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates an existing user and returns access and refresh tokens",
            security = {}
    )
    public ResponseEntity<SignInResponse> signin(@Valid @RequestBody SignInRequest request) {
        return ResponseEntity.ok(userAuthService.signIn(request));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Exchanges a valid refresh token for a new access token and rotated refresh token",
            security = {}
    )
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(userAuthService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Log out user",
            description = "Revokes the refresh-token session"
    )
    public ResponseEntity<LogoutResponse> logout(
            @RequestBody(required = false) LogoutRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        String refreshToken = request != null ? request.refreshToken() : null;
        return ResponseEntity.ok(userAuthService.logout(refreshToken, userId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('SCOPE_profile:read')")
    @Operation(
            summary = "Get current user profile",
            description = "Returns the authenticated user's profile and linked customer information"
    )
    public ResponseEntity<UserMeResponse> me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(userAuthService.getMe(userId));
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAuthority('SCOPE_profile:read')")
    @Operation(
            summary = "Update current user profile",
            description = "Updates permitted profile fields (first name, last name, phone number) for the authenticated user"
    )
    public ResponseEntity<UserMeResponse> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateMeRequest request
    ) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(userAuthService.updateMe(userId, request));
    }
}
