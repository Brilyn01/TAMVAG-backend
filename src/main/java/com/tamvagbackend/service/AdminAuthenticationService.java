package com.tamvagbackend.service;

import com.tamvagbackend.domain.entity.AdminUser;
import com.tamvagbackend.domain.repository.AdminUserRepository;
import com.tamvagbackend.dto.AdminAuthDtos.*;
import com.tamvagbackend.security.RolePermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
public class AdminAuthenticationService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final long adminAccessTokenTtlSeconds;

    public AdminAuthenticationService(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            @Value("${tamva.security.admin-access-token-ttl-seconds:3600}")
            long adminAccessTokenTtlSeconds
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.adminAccessTokenTtlSeconds = adminAccessTokenTtlSeconds;
    }

    @Transactional(readOnly = true)
    public AdminLoginResponse login(AdminLoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        AdminUser admin = adminUserRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid admin credentials"));

        if (!passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new BadCredentialsException("Invalid admin credentials");
        }

        if (!"ACTIVE".equalsIgnoreCase(admin.getStatus())) {
            throw new BadCredentialsException("Admin account is not active");
        }

        if (admin.getOperationalRole() == null || admin.getOperationalRole().isBlank()
                || !RolePermissions.isSupportedRole(admin.getOperationalRole())) {
            throw new BadCredentialsException("Admin account lacks a valid operational role");
        }

        Set<String> permissions = RolePermissions.getPermissionsForAdmin(
                admin.getRole(),
                admin.getOperationalRole()
        );

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(adminAccessTokenTtlSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("tamva")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(admin.getAdminUserId().toString())
                .claim("admin_user_id", admin.getAdminUserId().toString())
                .claim("email", admin.getEmail())
                .claim("role", admin.getRole())
                .claim("operational_role", admin.getOperationalRole())
                .claim("scope", String.join(" ", permissions))
                .claim("token_type", "admin")
                .build();

        String accessToken = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims
                )
        ).getTokenValue();

        return new AdminLoginResponse(
                accessToken,
                "Bearer",
                adminAccessTokenTtlSeconds,
                new AdminUserInfo(
                        admin.getAdminUserId(),
                        admin.getEmail(),
                        admin.getFirstName(),
                        admin.getLastName(),
                        admin.getRole(),
                        admin.getOperationalRole(),
                        admin.getStatus()
                ),
                permissions
        );
    }

    public AdminLogoutResponse logout() {
        return new AdminLogoutResponse("Admin logged out successfully");
    }
}

