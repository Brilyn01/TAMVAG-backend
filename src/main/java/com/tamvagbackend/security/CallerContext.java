package com.tamvagbackend.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public sealed interface CallerContext permits CallerContext.InstitutionCaller, CallerContext.AdminCaller {

    record InstitutionCaller(
            UUID institutionId,
            UUID applicationId
    ) implements CallerContext {
    }

    record AdminCaller(
            UUID adminUserId,
            String email,
            String role,
            String operationalRole
    ) implements CallerContext {
    }

    static CallerContext from(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication credentials are required"
            );
        }

        String tokenType = jwt.getClaimAsString("token_type");
        if (tokenType == null || tokenType.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token type claim is missing"
            );
        }

        switch (tokenType.toLowerCase()) {
            case "institution" -> {
                String institutionIdStr = jwt.getClaimAsString("institution_id");
                if (institutionIdStr == null || institutionIdStr.isBlank()) {
                    throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Institution token requires valid institution_id"
                    );
                }

                UUID institutionId;
                try {
                    institutionId = UUID.fromString(institutionIdStr);
                } catch (IllegalArgumentException e) {
                    throw new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Invalid institution_id claim format"
                    );
                }

                String applicationIdStr = jwt.getClaimAsString("application_id");
                UUID applicationId = null;
                if (applicationIdStr != null && !applicationIdStr.isBlank()) {
                    try {
                        applicationId = UUID.fromString(applicationIdStr);
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                return new InstitutionCaller(institutionId, applicationId);
            }

            case "admin" -> {
                String adminUserIdStr = jwt.getClaimAsString("admin_user_id");
                if (adminUserIdStr == null || adminUserIdStr.isBlank()) {
                    adminUserIdStr = jwt.getSubject();
                }

                UUID adminUserId = null;
                if (adminUserIdStr != null && !adminUserIdStr.isBlank()) {
                    try {
                        adminUserId = UUID.fromString(adminUserIdStr);
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                String email = jwt.getClaimAsString("email");
                String role = jwt.getClaimAsString("role");
                String operationalRole = jwt.getClaimAsString("operational_role");

                return new AdminCaller(adminUserId, email, role, operationalRole);
            }

            default -> throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Unsupported caller context for token type: " + tokenType
            );
        }
    }
}
