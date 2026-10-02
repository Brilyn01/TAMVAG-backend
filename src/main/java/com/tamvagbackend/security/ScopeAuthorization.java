package com.tamvagbackend.security;

public final class ScopeAuthorization {

    private ScopeAuthorization() {
    }

    public static final String RISK_EVALUATE =
            "SCOPE_risk:evaluate";

    public static final String PROFILE_READ =
            "SCOPE_profile:read";

    public static final String PROFILE_WRITE =
            "SCOPE_profile:write";

    public static final String CONSENT_CREATE =
            "SCOPE_consent:create";

    public static final String TRANSACTION_WRITE =
            "SCOPE_transaction:write";

    public static final String APPLICATION_READ =
            "SCOPE_application:read";

    public static final String APPLICATION_MANAGE =
            "SCOPE_application:manage";

    public static final String CONNECTOR_READ =
            "SCOPE_connector:read";

    public static final String CONNECTOR_SYNC =
            "SCOPE_connector:sync";

    public static final String CASE_READ =
            "SCOPE_cases:read";

    public static final String CASE_WRITE =
            "SCOPE_cases:write";

    public static final String CASE_CREATE =
            "SCOPE_cases:create";

    public static final String CASE_ASSIGN =
            "SCOPE_cases:assign";

    public static final String CASE_CLOSE =
            "SCOPE_cases:close";

    public static final String AUDIT_READ =
            "SCOPE_audit:read";

    public static final String WALLET_READ =
            "SCOPE_wallet:read";

    public static final String WALLET_WRITE =
            "SCOPE_wallet:write";

    public static final String WEBHOOK_READ =
            "SCOPE_webhook:read";

    public static final String WEBHOOK_MANAGE =
            "SCOPE_webhook:manage";
}