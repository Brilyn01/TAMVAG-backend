package com.tamvagbackend.security;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class RolePermissions {

    private RolePermissions() {
    }

    /*
     * Operational roles identified in the requirements.
     */
    public static final String RISK_ANALYST = "RISK_ANALYST";
    public static final String SECURITY_ADMINISTRATOR = "SECURITY_ADMINISTRATOR";
    public static final String PLATFORM_OPERATOR = "PLATFORM_OPERATOR";

    public static final String ADMIN = "ADMIN";
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    /*
     * Explicit permissions / scopes
     */
    public static final String CASES_READ = "cases:read";
    public static final String CASES_WRITE = "cases:write";
    public static final String CASES_CREATE = "cases:create";
    public static final String CASES_ASSIGN = "cases:assign";
    public static final String CASES_CLOSE = "cases:close";
    public static final String PROFILE_READ = "profile:read";
    public static final String RISK_EVALUATE = "risk:evaluate";
    public static final String APPLICATION_READ = "application:read";
    public static final String APPLICATION_MANAGE = "application:manage";
    public static final String CONNECTOR_READ = "connector:read";
    public static final String CONNECTOR_SYNC = "connector:sync";
    public static final String AUDIT_READ = "audit:read";
    public static final String ADMIN_PROVISION = "admin:provision";
    public static final String ADMIN_MANAGE = "admin:manage";

    /*
     * Normalize a role supplied by a trusted identity source.
     */
    public static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException(
                    "Role is required"
            );
        }

        return role.trim()
                .toUpperCase(Locale.ROOT);
    }

    /*
     * Validate the supported operational roles.
     */
    public static boolean isSupportedRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }

        String normalizedRole = normalizeRole(role);

        return switch (normalizedRole) {
            case RISK_ANALYST,
                 SECURITY_ADMINISTRATOR,
                 PLATFORM_OPERATOR -> true;
            default -> false;
        };
    }

    public static boolean isSupportedAdministrativeRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }

        String normalizedRole = normalizeRole(role);

        return switch (normalizedRole) {
            case ADMIN, SUPER_ADMIN -> true;
            default -> false;
        };
    }

    /*
     * Compute explicit permissions for an administrative user.
     */
    public static Set<String> getPermissionsForAdmin(String role, String operationalRole) {
        Set<String> permissions = new HashSet<>();

        String normRole = role != null ? normalizeRole(role) : ADMIN;
        String normOpRole = operationalRole != null && !operationalRole.isBlank()
                ? normalizeRole(operationalRole)
                : null;

        if (SUPER_ADMIN.equals(normRole)) {
            permissions.addAll(Set.of(
                    CASES_READ, CASES_WRITE, CASES_CREATE, CASES_ASSIGN, CASES_CLOSE,
                    PROFILE_READ, RISK_EVALUATE,
                    APPLICATION_READ, APPLICATION_MANAGE,
                    CONNECTOR_READ, CONNECTOR_SYNC,
                    AUDIT_READ, ADMIN_PROVISION, ADMIN_MANAGE
            ));
            return Collections.unmodifiableSet(permissions);
        }

        if (normOpRole != null) {
            switch (normOpRole) {
                case RISK_ANALYST -> permissions.addAll(Set.of(
                        CASES_READ, CASES_WRITE, PROFILE_READ, RISK_EVALUATE, AUDIT_READ
                ));
                case SECURITY_ADMINISTRATOR -> permissions.addAll(Set.of(
                        CASES_READ, CASES_WRITE, CASES_CREATE, CASES_ASSIGN, CASES_CLOSE, AUDIT_READ
                ));
                case PLATFORM_OPERATOR -> permissions.addAll(Set.of(
                        APPLICATION_READ, APPLICATION_MANAGE, CONNECTOR_READ, CONNECTOR_SYNC, AUDIT_READ
                ));
            }
        }

        return Collections.unmodifiableSet(permissions);
    }
}