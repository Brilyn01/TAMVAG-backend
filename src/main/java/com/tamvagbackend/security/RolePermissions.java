package com.tamvagbackend.security;

import java.util.Locale;

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
     * Normalize a role supplied by a trusted identity source.
     *
     * This method does not authorize the role or grant permissions.
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
}